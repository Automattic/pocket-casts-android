package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartRulesTest {
    private val clock = MutableClock()

    @Test
    fun `unplayed episodes`() {
        val rule = SmartRules.EpisodeStatusRule(
            unplayed = true,
            inProgress = false,
            completed = false,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.playing_status IN (0)", clause)
    }

    @Test
    fun `in progress episodes`() {
        val rule = SmartRules.EpisodeStatusRule(
            unplayed = false,
            inProgress = true,
            completed = false,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.playing_status IN (1)", clause)
    }

    @Test
    fun `completed episodes`() {
        val rule = SmartRules.EpisodeStatusRule(
            unplayed = false,
            inProgress = false,
            completed = true,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.playing_status IN (2)", clause)
    }

    @Test
    fun `mixed episode played statuses`() {
        val rule = SmartRules.EpisodeStatusRule(
            unplayed = true,
            inProgress = false,
            completed = true,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.playing_status IN (0,2)", clause)
    }

    @Test
    fun `all episode played statuses`() {
        val rule = SmartRules.EpisodeStatusRule(
            unplayed = true,
            inProgress = true,
            completed = true,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `no episode played statuses`() {
        val rule = SmartRules.EpisodeStatusRule(
            unplayed = false,
            inProgress = false,
            completed = false,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `downloaded episodes`() {
        val rule = SmartRules.DownloadStatusRule.Downloaded

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.episode_status IN (4)", clause)
    }

    @Test
    fun `not downloaded episodes`() {
        val rule = SmartRules.DownloadStatusRule.NotDownloaded

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.episode_status IN (2,1,6,5,0,3)", clause)
    }

    @Test
    fun `all episode download statuses`() {
        val rule = SmartRules.DownloadStatusRule.Any

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `audio episodes`() {
        val rule = SmartRules.MediaTypeRule.Audio

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.file_type LIKE 'audio/%'", clause)
    }

    @Test
    fun `video episodes`() {
        val rule = SmartRules.MediaTypeRule.Video

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.file_type LIKE 'video/%'", clause)
    }

    @Test
    fun `all media type episodes`() {
        val rule = SmartRules.MediaTypeRule.Any

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `episodes released day ago`() {
        val rule = SmartRules.ReleaseDateRule.Last24Hours

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.published_date > -${1.days.inWholeMilliseconds}", clause)
    }

    @Test
    fun `episodes released 3 day ago`() {
        val rule = SmartRules.ReleaseDateRule.Last3Days

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.published_date > -${3.days.inWholeMilliseconds}", clause)
    }

    @Test
    fun `episodes released 7 day ago`() {
        val rule = SmartRules.ReleaseDateRule.LastWeek

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.published_date > -${7.days.inWholeMilliseconds}", clause)
    }

    @Test
    fun `episodes released 14 day ago`() {
        val rule = SmartRules.ReleaseDateRule.Last2Weeks

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.published_date > -${14.days.inWholeMilliseconds}", clause)
    }

    @Test
    fun `episodes released 31 day ago`() {
        val rule = SmartRules.ReleaseDateRule.LastMonth

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.published_date > -${31.days.inWholeMilliseconds}", clause)
    }

    @Test
    fun `episodes released any time ago`() {
        val rule = SmartRules.ReleaseDateRule.AnyTime

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `starred episodes`() {
        val rule = SmartRules.StarredRule.Starred

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.starred = 1", clause)
    }

    @Test
    fun `any starred episode status`() {
        val rule = SmartRules.StarredRule.Any

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `selected podcasts`() {
        val rule = SmartRules.PodcastsRule.Selected(
            uuids = setOf("id-1", "id-2"),
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("episode.podcast_id IN ('id-1','id-2')", clause)
    }

    @Test
    fun `all podcasts`() {
        val rule = SmartRules.PodcastsRule.Any

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `constrained episode duration`() {
        val rule = SmartRules.EpisodeDurationRule.Constrained(
            longerThan = 128.seconds,
            shorterThan = 200.seconds,
        )

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("(episode.duration BETWEEN 128 AND 200)", clause)
    }

    @Test
    fun `any episode duration`() {
        val rule = SmartRules.EpisodeDurationRule.Any

        val clause = rule.toSqlWhereClause(clock)

        assertEquals("", clause)
    }

    @Test
    fun `default rules`() {
        val rules = SmartRules.Default

        val clause = rules.toSqlWhereClause(clock)

        assertEquals("(episode.archived = 0) AND (podcast.subscribed = 1)", clause)
    }

    @Test
    fun `multiple rules`() {
        val rules = SmartRules.Default.copy(
            downloadStatus = SmartRules.DownloadStatusRule.Downloaded,
            mediaType = SmartRules.MediaTypeRule.Video,
            episodeDuration = SmartRules.EpisodeDurationRule.Constrained(
                longerThan = 10.seconds,
                shorterThan = 50.seconds,
            ),
        )

        val clause = rules.toSqlWhereClause(clock)

        assertEquals("(episode.episode_status IN (4)) AND (episode.file_type LIKE 'video/%') AND ((episode.duration BETWEEN 10 AND 50)) AND (episode.archived = 0) AND (podcast.subscribed = 1)", clause)
    }
}
