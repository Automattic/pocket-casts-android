package au.com.shiftyjelly.pocketcasts.playlists.create

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.playlists.smart.RulesBuilder
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class RulesBuilderTest {
    @Test
    fun `change min episode duration`() {
        var builder = RulesBuilder.Empty.copy(
            minEpisodeDuration = 15.minutes,
            maxEpisodeDuration = 20.minutes,
        )

        builder = builder.incrementMinDuration()
        assertEquals(15.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(10.minutes, builder.minEpisodeDuration)

        builder = builder.incrementMinDuration()
        assertEquals(15.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(10.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(5.minutes, builder.minEpisodeDuration)

        builder = builder.incrementMinDuration()
        assertEquals(10.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(5.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(4.minutes, builder.minEpisodeDuration)

        builder = builder.incrementMinDuration()
        assertEquals(5.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(4.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(3.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(2.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(1.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(0.minutes, builder.minEpisodeDuration)

        builder = builder.decrementMinDuration()
        assertEquals(0.minutes, builder.minEpisodeDuration)
    }

    @Test
    fun `change max episode duration`() {
        var builder = RulesBuilder.Empty.copy(
            minEpisodeDuration = 0.minutes,
            maxEpisodeDuration = 1.minutes,
        )

        builder = builder.decrementMaxDuration()
        assertEquals(1.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(2.minutes, builder.maxEpisodeDuration)

        builder = builder.decrementMaxDuration()
        assertEquals(1.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(2.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(3.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(4.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(5.minutes, builder.maxEpisodeDuration)

        builder = builder.decrementMaxDuration()
        assertEquals(4.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(5.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(10.minutes, builder.maxEpisodeDuration)

        builder = builder.decrementMaxDuration()
        assertEquals(5.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(10.minutes, builder.maxEpisodeDuration)

        builder = builder.incrementMaxDuration()
        assertEquals(15.minutes, builder.maxEpisodeDuration)
    }

    @Test
    fun `apply smart rules without overriding selectable content`() {
        val builder = RulesBuilder(
            useAllPodcasts = false,
            selectedPodcasts = setOf("id-1", "id-2"),
            episodeStatusRule = EpisodeStatusRule(
                unplayed = false,
                inProgress = false,
                completed = true,
            ),
            releaseDateRule = ReleaseDateRule.Last3Days,
            isEpisodeDurationConstrained = true,
            minEpisodeDuration = 22.minutes,
            maxEpisodeDuration = 75.minutes,
            downloadStatusRule = DownloadStatusRule.Downloaded,
            mediaTypeRule = MediaTypeRule.Video,
            useStarredEpisode = true,
        )

        val appliedBuilder = builder.applyRules(SmartRules.Default)

        assertEquals(
            RulesBuilder(
                useAllPodcasts = true,
                selectedPodcasts = setOf("id-1", "id-2"),
                episodeStatusRule = EpisodeStatusRule(
                    unplayed = true,
                    inProgress = true,
                    completed = true,
                ),
                releaseDateRule = ReleaseDateRule.AnyTime,
                isEpisodeDurationConstrained = false,
                minEpisodeDuration = 22.minutes,
                maxEpisodeDuration = 75.minutes,
                downloadStatusRule = DownloadStatusRule.Any,
                mediaTypeRule = MediaTypeRule.Any,
                useStarredEpisode = false,
            ),
            appliedBuilder,
        )
    }

    @Test
    fun `apply smart rules with overriding selectable content`() {
        val rules = SmartRules.Default.copy(
            podcasts = PodcastsRule.Selected(setOf("id-1", "id-3")),
            episodeDuration = EpisodeDurationRule.Constrained(28.minutes, 30.minutes),
        )

        val appliedBuilder = RulesBuilder.Empty.applyRules(rules)

        assertEquals(
            RulesBuilder(
                useAllPodcasts = false,
                selectedPodcasts = setOf("id-1", "id-3"),
                episodeStatusRule = EpisodeStatusRule(
                    unplayed = true,
                    inProgress = true,
                    completed = true,
                ),
                releaseDateRule = ReleaseDateRule.AnyTime,
                isEpisodeDurationConstrained = true,
                minEpisodeDuration = 28.minutes,
                maxEpisodeDuration = 30.minutes,
                downloadStatusRule = DownloadStatusRule.Any,
                mediaTypeRule = MediaTypeRule.Any,
                useStarredEpisode = false,
            ),
            appliedBuilder,
        )
    }
}
