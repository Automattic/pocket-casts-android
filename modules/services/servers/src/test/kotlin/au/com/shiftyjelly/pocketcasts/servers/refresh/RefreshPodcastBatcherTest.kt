package au.com.shiftyjelly.pocketcasts.servers.refresh

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.servers.RefreshResponse
import au.com.shiftyjelly.pocketcasts.servers.ServerManager.Parameters
import java.lang.RuntimeException
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshPodcastBatcherTest {
    @Test
    fun `do not allow negative batch size`() {
        try {
            RefreshPodcastBatcher(-10)
        } catch (e: IllegalArgumentException) {
            assertEquals("Batch size must be positive: -10", e.message)
        }
    }

    @Test
    fun `do not allow 0 batch size`() {
        try {
            RefreshPodcastBatcher(0)
        } catch (e: IllegalArgumentException) {
            assertEquals("Batch size must be positive: 0", e.message)
        }
    }

    @Test
    fun `partition podcasts into correct batches`() = runTest {
        val batcher = RefreshPodcastBatcher(3)
        val podcasts = List(10) { Podcast(uuid = UUID.randomUUID().toString()) }
        val podcastsIds = podcasts.map(Podcast::uuid)

        val parameterList = mutableListOf<Parameters>()
        batcher.refreshPodcasts(podcasts) { parameters ->
            parameterList += parameters
            null
        }

        assertEquals(podcastsIds.subList(0, 3).joinToString(","), parameterList[0]["podcasts"])
        assertEquals(podcastsIds.subList(3, 6).joinToString(","), parameterList[1]["podcasts"])
        assertEquals(podcastsIds.subList(6, 9).joinToString(","), parameterList[2]["podcasts"])
        assertEquals(podcastsIds.subList(9, 10).joinToString(","), parameterList[3]["podcasts"])
    }

    @Test
    fun `partition episodes into correct batches`() = runTest {
        val batcher = RefreshPodcastBatcher(4)
        val podcasts = List(10) { Podcast(uuid = UUID.randomUUID().toString(), latestEpisodeUuid = UUID.randomUUID().toString()) }
        val episodeIds = podcasts.map(Podcast::latestEpisodeUuid)

        val parameterList = mutableListOf<Parameters>()
        batcher.refreshPodcasts(podcasts) { parameters ->
            parameterList += parameters
            null
        }

        assertEquals(episodeIds.subList(0, 4).joinToString(","), parameterList[0]["last_episodes"])
        assertEquals(episodeIds.subList(4, 8).joinToString(","), parameterList[1]["last_episodes"])
        assertEquals(episodeIds.subList(8, 10).joinToString(","), parameterList[2]["last_episodes"])
    }

    @Test
    fun `return combined responses`() = runTest {
        val batcher = RefreshPodcastBatcher(1)
        val podcasts = List(2) { Podcast(uuid = UUID.randomUUID().toString()) }

        val responses = mapOf(
            0 to RefreshResponse().apply {
                addUpdate(
                    "podcast-1",
                    listOf(
                        PodcastEpisode("episode-1", publishedDate = Date()),
                        PodcastEpisode("episode-2", publishedDate = Date()),
                    ),
                )
            },
            1 to RefreshResponse().apply {
                addUpdate(
                    "podcast-2",
                    listOf(
                        PodcastEpisode("episode-1", publishedDate = Date()),
                        PodcastEpisode("episode-2", publishedDate = Date()),
                        PodcastEpisode("episode-3", publishedDate = Date()),
                    ),
                )
            },
        )

        var callCount = 0
        val result = batcher.refreshPodcasts(podcasts) { responses[callCount++] }

        assertEquals(responses.getValue(0).merge(responses.getValue(1)), result.getOrThrow())
    }

    @Test
    fun `ignore null responses`() = runTest {
        val batcher = RefreshPodcastBatcher(1)
        val podcasts = List(3) { Podcast(uuid = UUID.randomUUID().toString()) }

        val responses = mapOf(
            0 to RefreshResponse().apply {
                addUpdate(
                    "podcast-1",
                    listOf(
                        PodcastEpisode("episode-1", publishedDate = Date()),
                    ),
                )
            },
            1 to RefreshResponse().apply {
                addUpdate(
                    "podcast-2",
                    listOf(
                        PodcastEpisode("episode-2", publishedDate = Date()),
                    ),
                )
            },
        )

        var callCount = 0
        val result = batcher.refreshPodcasts(podcasts) { responses[callCount++] }

        assertEquals(responses.getValue(0).merge(responses.getValue(1)), result.getOrThrow())
    }

    @Test
    fun `return null result for empty podcasts`() = runTest {
        val batcher = RefreshPodcastBatcher(1)

        val result = batcher.refreshPodcasts(emptyList()) { error("Unexpected call") }

        assertNull(result.getOrThrow())
    }

    @Test
    fun `return null if all responses are null`() = runTest {
        val batcher = RefreshPodcastBatcher(1)
        val podcasts = List(10) { Podcast(uuid = UUID.randomUUID().toString()) }

        val result = batcher.refreshPodcasts(podcasts) { null }

        assertNull(result.getOrThrow())
    }

    @Test
    fun `fail if any of the responses fails`() = runTest {
        val batcher = RefreshPodcastBatcher(1)
        val podcasts = List(10) { Podcast(uuid = UUID.randomUUID().toString()) }
        val exception = EqualsException("Hello, there!")

        var callCount = 0
        val result = batcher.refreshPodcasts(podcasts) {
            if (callCount++ == 5) {
                throw exception
            } else {
                null
            }
        }

        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `do not proceed with calls after first failure`() = runTest {
        val batcher = RefreshPodcastBatcher(1)
        val podcasts = List(10) { Podcast(uuid = UUID.randomUUID().toString()) }
        val exception = EqualsException("Hello, there!")

        var callCount = 0
        batcher.refreshPodcasts(podcasts) {
            if (callCount++ == 5) {
                throw exception
            } else {
                null
            }
        }

        assertEquals(6, callCount)
    }

    private class EqualsException(message: String) : RuntimeException(message) {
        override fun equals(other: Any?) = other is EqualsException && other.message == this.message

        override fun hashCode() = message.hashCode()

        override fun toString() = "EqualsException($message)"
    }
}
