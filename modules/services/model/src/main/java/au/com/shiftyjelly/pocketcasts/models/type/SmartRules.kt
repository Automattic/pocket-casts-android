package au.com.shiftyjelly.pocketcasts.models.type

import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

data class SmartRules(
    val episodeStatus: EpisodeStatusRule,
    val downloadStatus: DownloadStatusRule,
    val mediaType: MediaTypeRule,
    val releaseDate: ReleaseDateRule,
    val starred: StarredRule,
    val podcasts: PodcastsRule,
    val episodeDuration: EpisodeDurationRule,
) {
    fun toSqlWhereClause(clock: Clock) = buildString {
        episodeStatus.run { appendSqlQuery(clock) }
        downloadStatus.run { appendSqlQuery(clock) }
        mediaType.run { appendSqlQuery(clock) }
        releaseDate.run { appendSqlQuery(clock) }
        starred.run { appendSqlQuery(clock) }
        podcasts.run { appendSqlQuery(clock) }
        episodeDuration.run { appendSqlQuery(clock) }
        NonArchivedRule.run { appendSqlQuery(clock) }
        FollowedPodcastRule.run { appendSqlQuery(clock) }
    }

    fun analyticsProperties() = buildMap {
        putAll(episodeStatus.analyticsProperties())
        putAll(downloadStatus.analyticsProperties())
        putAll(mediaType.analyticsProperties())
        putAll(releaseDate.analyticsProperties())
        putAll(starred.analyticsProperties())
        putAll(podcasts.analyticsProperties())
        putAll(episodeDuration.analyticsProperties())
    }

    data class EpisodeStatusRule(
        val unplayed: Boolean,
        val inProgress: Boolean,
        val completed: Boolean,
    ) : SmartRule {
        override fun toSqlWhereClause(clock: Clock) = buildString {
            if (!areAllConstraintsTicked && isAnyConstraintTicked) {
                val statuses = buildList {
                    if (unplayed) {
                        add(EpisodePlayingStatus.NOT_PLAYED)
                    }
                    if (inProgress) {
                        add(EpisodePlayingStatus.IN_PROGRESS)
                    }
                    if (completed) {
                        add(EpisodePlayingStatus.COMPLETED)
                    }
                }
                append("episode.playing_status IN (")
                append(statuses.joinToString(separator = ",") { status -> "${status.ordinal}" })
                append(')')
            }
        }

        fun analyticsProperties() = mapOf(
            "episode_status_in_progress" to inProgress,
            "episode_status_played" to completed,
            "episode_status_unplayed" to unplayed,
        )

        private val areAllConstraintsTicked get() = unplayed && inProgress && completed

        private val isAnyConstraintTicked get() = unplayed || inProgress || completed
    }

    enum class DownloadStatusRule : SmartRule {
        Any,
        Downloaded,
        NotDownloaded,
        ;

        override fun toSqlWhereClause(clock: Clock) = buildString {
            val statuses = when (this@DownloadStatusRule) {
                Any -> return@buildString
                Downloaded -> listOf(EpisodeStatusEnum.DOWNLOADED)
                NotDownloaded -> listOf(
                    EpisodeStatusEnum.DOWNLOADING,
                    EpisodeStatusEnum.QUEUED,
                    EpisodeStatusEnum.WAITING_FOR_POWER,
                    EpisodeStatusEnum.WAITING_FOR_WIFI,
                    EpisodeStatusEnum.NOT_DOWNLOADED,
                    EpisodeStatusEnum.DOWNLOAD_FAILED,
                )
            }
            append("episode.episode_status IN (")
            append(statuses.joinToString(separator = ",") { status -> "${status.ordinal}" })
            append(')')
        }

        fun analyticsProperties() = mapOf(
            "downloaded" to (this == Any || this == Downloaded),
            "not_downloaded" to (this == Any || this == NotDownloaded),
        )
    }

    enum class MediaTypeRule : SmartRule {
        Any,
        Audio,
        Video,
        ;

        override fun toSqlWhereClause(clock: Clock) = buildString {
            when (this@MediaTypeRule) {
                Any -> Unit
                Audio -> append("episode.file_type LIKE 'audio/%'")
                Video -> append("episode.file_type LIKE 'video/%'")
            }
        }

        fun analyticsProperties() = mapOf(
            "media_type" to when (this) {
                Any -> "all"
                Audio -> "audio"
                Video -> "video"
            },
        )
    }

    enum class ReleaseDateRule(
        private val duration: Duration,
    ) : SmartRule {
        AnyTime(
            duration = Duration.INFINITE,
        ),
        Last24Hours(
            duration = 1.days,
        ),
        Last3Days(
            duration = 3.days,
        ),
        LastWeek(
            duration = 7.days,
        ),
        Last2Weeks(
            duration = 14.days,
        ),
        LastMonth(
            duration = 31.days,
        ),
        ;

        override fun toSqlWhereClause(clock: Clock) = buildString {
            if (duration != Duration.INFINITE) {
                append("episode.published_date > ")
                append(clock.instant().minusMillis(duration.inWholeMilliseconds).toEpochMilli())
            }
        }

        fun analyticsProperties() = mapOf(
            "release_date" to when (this) {
                AnyTime -> "anytime"
                Last24Hours -> "24_hours"
                Last3Days -> "3_days"
                LastWeek -> "last_week"
                Last2Weeks -> "last_2_weeks"
                LastMonth -> "last_month"
            },
        )
    }

    enum class StarredRule : SmartRule {
        Any,
        Starred,
        ;

        override fun toSqlWhereClause(clock: Clock) = buildString {
            when (this@StarredRule) {
                Any -> Unit
                Starred -> append("episode.starred = 1")
            }
        }

        fun analyticsProperties() = mapOf(
            "starred" to (this == Starred),
        )
    }

    sealed interface PodcastsRule : SmartRule {
        data object Any : PodcastsRule {
            override fun toSqlWhereClause(clock: Clock) = ""
        }

        data class Selected(
            val uuids: Set<String>,
        ) : PodcastsRule {
            fun withPodcast(podcastUuid: String) = copy(uuids + podcastUuid)

            fun withoutPodcast(podcastUuid: String) = copy(uuids - podcastUuid)

            override fun toSqlWhereClause(clock: Clock) = buildString {
                append("episode.podcast_id IN (")
                append(uuids.joinToString(separator = ",") { uuid -> "'$uuid'" })
                append(')')
            }
        }

        fun analyticsProperties() = mapOf(
            "all_podcasts" to (this is Any),
        )
    }

    sealed interface EpisodeDurationRule : SmartRule {

        fun analyticsProperties(): Map<String, kotlin.Any>

        data object Any : EpisodeDurationRule {
            override fun toSqlWhereClause(clock: Clock) = ""

            override fun analyticsProperties() = mapOf(
                "duration" to false,
            )
        }

        data class Constrained(
            val longerThan: Duration,
            val shorterThan: Duration,
        ) : EpisodeDurationRule {
            override fun toSqlWhereClause(clock: Clock) = buildString {
                append("(episode.duration BETWEEN ")
                append(longerThan.inWholeSeconds)
                append(" AND ")
                append(shorterThan.inWholeSeconds)
                append(')')
            }

            override fun analyticsProperties() = mapOf(
                "duration" to true,
                "duration_longer_than" to longerThan.inWholeMinutes,
                "duration_shorter_than" to shorterThan.inWholeMinutes,
            )
        }
    }

    data object NonArchivedRule : SmartRule {
        override fun toSqlWhereClause(clock: Clock) = "episode.archived = 0"
    }

    data object FollowedPodcastRule : SmartRule {
        override fun toSqlWhereClause(clock: Clock) = "podcast.subscribed = 1"
    }

    sealed interface SmartRule {
        fun StringBuilder.appendSqlQuery(clock: Clock) {
            val sqlString = toSqlWhereClause(clock)
            if (sqlString.isNotEmpty()) {
                if (isNotEmpty()) {
                    append(" AND ")
                }
                append('(')
                append(sqlString)
                append(')')
            }
        }

        fun toSqlWhereClause(clock: Clock): String
    }

    companion object {
        val Default = SmartRules(
            EpisodeStatusRule(unplayed = true, inProgress = true, completed = true),
            DownloadStatusRule.Any,
            MediaTypeRule.Any,
            ReleaseDateRule.AnyTime,
            StarredRule.Any,
            PodcastsRule.Any,
            EpisodeDurationRule.Any,
        )
    }
}
