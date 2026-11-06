package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import au.com.shiftyjelly.pocketcasts.models.to.PlaybackPodcastStats
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.PlaybackStats
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import com.automattic.pocketcasts.playback.PlaybackPayload
import java.time.Year
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Playback2026PayloadTest {
    private val year = Year.of(2026)

    @Test
    fun `map podcasts to payload podcasts`() {
        val stats = stats(
            podcasts = listOf(
                podcastStats(uuid = "uuid-1", categories = "Comedy\nSociety & Culture", playedSeconds = 3600.0, episodeCount = 4),
                podcastStats(uuid = "uuid-2", categories = "", playedSeconds = 60.0, episodeCount = 1),
            ),
        )

        val payload = stats.toPlaybackPayload(year)

        assertEquals(2026, payload.year)
        assertEquals(
            listOf(
                PlaybackPayload.Podcast(
                    uuid = "uuid-1",
                    title = "Title uuid-1",
                    author = "Author uuid-1",
                    genre = "Comedy",
                    artworkUrl = PodcastImage.getMediumArtworkUrl("uuid-1"),
                    playedSeconds = 3600.0,
                    episodesPlayed = 4,
                ),
                PlaybackPayload.Podcast(
                    uuid = "uuid-2",
                    title = "Title uuid-2",
                    author = "Author uuid-2",
                    genre = "Other",
                    artworkUrl = PodcastImage.getMediumArtworkUrl("uuid-2"),
                    playedSeconds = 60.0,
                    episodesPlayed = 1,
                ),
            ),
            payload.podcasts,
        )
    }

    @Test
    fun `pass monthly seconds, previous year and completion`() {
        val monthlySeconds = List(12) { it * 100.0 }
        val stats = stats(
            monthlySeconds = monthlySeconds,
            lastYearSeconds = 500.0,
            startedEpisodeCount = 10,
            completedEpisodeCount = 7,
        )

        val payload = stats.toPlaybackPayload(year)

        assertEquals(monthlySeconds, payload.monthlySeconds)
        assertEquals(500.0, payload.previousYearSeconds)
        assertEquals(PlaybackPayload.Completion(started = 10, completed = 7), payload.completion)
    }

    @Test
    fun `leave out previous year and completion without listening`() {
        val payload = stats(lastYearSeconds = 0.0, startedEpisodeCount = 0, completedEpisodeCount = 0).toPlaybackPayload(year)

        assertNull(payload.previousYearSeconds)
        assertNull(payload.completion)
    }

    private fun stats(
        podcasts: List<PlaybackPodcastStats> = emptyList(),
        monthlySeconds: List<Double> = List(12) { 0.0 },
        lastYearSeconds: Double = 0.0,
        startedEpisodeCount: Int = 0,
        completedEpisodeCount: Int = 0,
    ) = PlaybackStats(
        podcasts = podcasts,
        monthlySeconds = monthlySeconds,
        lastYearSeconds = lastYearSeconds,
        startedEpisodeCount = startedEpisodeCount,
        completedEpisodeCount = completedEpisodeCount,
    )

    private fun podcastStats(
        uuid: String,
        categories: String,
        playedSeconds: Double,
        episodeCount: Int,
    ) = PlaybackPodcastStats(
        uuid = uuid,
        title = "Title $uuid",
        author = "Author $uuid",
        categories = categories,
        playedSeconds = playedSeconds,
        playedEpisodeCount = episodeCount,
    )
}
