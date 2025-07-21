package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import java.time.Clock
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

data class SmartRules(
    val episodeStatus: EpisodeStatusRule,
    val downloadStatus: DownloadStatusRule,
    val mediaType: MediaTypeRule,
    val releaseDate: ReleaseDateRule,
    val starred: StarredRule,
    val podcastsRule: PodcastsRule,
    val episodeDuration: EpisodeDurationRule,
) {
    fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
        episodeStatus.run { append(clock, playlistId) }
        downloadStatus.run { append(clock, playlistId) }
        mediaType.run { append(clock, playlistId) }
        releaseDate.run { append(clock, playlistId) }
        starred.run { append(clock, playlistId) }
        podcastsRule.run { append(clock, playlistId) }
        episodeDuration.run { append(clock, playlistId) }
        NonArchivedRule.run { append(clock, playlistId) }
    }

    data class EpisodeStatusRule(
        val unplayed: Boolean,
        val inProgres: Boolean,
        val completed: Boolean,
    ) : SmartRule {
        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            if (!areAllConstraintsTicked && isAnyConstraintTicked) {
                val statuses = buildList {
                    if (unplayed) {
                        add(EpisodePlayingStatus.NOT_PLAYED)
                    }
                    if (inProgres) {
                        add(EpisodePlayingStatus.IN_PROGRESS)
                    }
                    if (completed) {
                        add(EpisodePlayingStatus.COMPLETED)
                    }
                }
                append("playing_status IN (")
                statuses.forEachIndexed { index, status ->
                    if (index != 0) {
                        append(',')
                    }
                    append(status.ordinal)
                }
                append(')')
            }
        }

        private val areAllConstraintsTicked get() = unplayed && inProgres && completed

        private val isAnyConstraintTicked get() = unplayed || inProgres || completed
    }

    enum class DownloadStatusRule : SmartRule {
        Any,
        Downloaded,
        NotDownloaded,
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            if (this@DownloadStatusRule != Any) {
                val isSystemDownloadsPlaylist = playlistId == SmartPlaylist.PLAYLIST_ID_SYSTEM_DOWNLOADS
                val includeDownloaded = this@DownloadStatusRule == Downloaded
                val includeDownloading = isSystemDownloadsPlaylist || this@DownloadStatusRule == NotDownloaded
                val includeNotDownloaded = this@DownloadStatusRule == NotDownloaded

                val statuses = buildList {
                    if (includeDownloaded) {
                        add(EpisodeStatusEnum.DOWNLOADED)
                    }
                    if (includeDownloading) {
                        add(EpisodeStatusEnum.DOWNLOADING)
                        add(EpisodeStatusEnum.QUEUED)
                        add(EpisodeStatusEnum.WAITING_FOR_POWER)
                        add(EpisodeStatusEnum.WAITING_FOR_WIFI)
                    }
                    if (includeNotDownloaded) {
                        add(EpisodeStatusEnum.NOT_DOWNLOADED)
                        add(EpisodeStatusEnum.DOWNLOAD_FAILED)
                    }
                }
                append("episode_status IN (")
                statuses.forEachIndexed { index, status ->
                    if (index != 0) {
                        append(',')
                    }
                    append(status.ordinal)
                }
                append(')')

                if (isSystemDownloadsPlaylist) {
                    if (isNotEmpty()) {
                        append(" OR ")
                    }
                    append("(episode_status = ")
                    append(EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal)
                    append(" AND last_download_attempt_date > ")
                    append(clock.instant().minus(7, ChronoUnit.DAYS).toEpochMilli())
                    append(')')
                }
            }
        }
    }

    enum class MediaTypeRule : SmartRule {
        Any,
        Audio,
        Video,
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            when (this@MediaTypeRule) {
                Any -> Unit
                Audio -> append("file_type LIKE 'audio/%'")
                Video -> append("file_type LIKE 'video/%'")
            }
        }
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

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            if (duration != Duration.INFINITE) {
                append("published_date > ")
                append(clock.instant().minusMillis(duration.inWholeMilliseconds).toEpochMilli())
            }
        }
    }

    enum class StarredRule : SmartRule {
        Any,
        Starred,
        ;

        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
            when (this@StarredRule) {
                Any -> Unit
                Starred -> append("starred = 1")
            }
        }
    }

    sealed interface PodcastsRule : SmartRule {
        data object Any : PodcastsRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = ""
        }

        data class Selected(
            val uuids: List<String>,
        ) : PodcastsRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
                append("podcast_id IN (")
                uuids.forEachIndexed { index, uuid ->
                    if (index != 0) {
                        append(',')
                    }
                    append('\'')
                    append(uuid)
                    append('\'')
                }
                append(')')
            }
        }
    }

    sealed interface EpisodeDurationRule : SmartRule {
        data object Any : EpisodeDurationRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = ""
        }

        data class Constrained(
            val longerThan: Duration,
            val shorterThan: Duration,
        ) : EpisodeDurationRule {
            override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = buildString {
                append("(duration BETWEEN ")
                append(longerThan.inWholeSeconds)
                append(" AND ")
                append(shorterThan.inWholeSeconds)
                append(')')
            }
        }
    }

    data object NonArchivedRule : SmartRule {
        override fun toSqlWhereClause(clock: Clock, playlistId: Long?) = "archived = 0"
    }

    sealed interface SmartRule {
        fun StringBuilder.append(clock: Clock, playlistId: Long?) {
            val sqlString = toSqlWhereClause(clock, playlistId)
            if (sqlString.isNotEmpty()) {
                if (isNotEmpty()) {
                    append(" AND ")
                }
                append('(')
                append(sqlString)
                append(')')
            }
        }

        fun toSqlWhereClause(clock: Clock, playlistId: Long?): String
    }

    companion object {
        val Default = SmartRules(
            EpisodeStatusRule(unplayed = true, inProgres = true, completed = true),
            DownloadStatusRule.Any,
            MediaTypeRule.Any,
            ReleaseDateRule.AnyTime,
            StarredRule.Any,
            PodcastsRule.Any,
            EpisodeDurationRule.Any,
        )
    }
}
