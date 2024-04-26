package au.com.shiftyjelly.pocketcasts.servers.refresh

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.RefreshResponse
import au.com.shiftyjelly.pocketcasts.servers.ServerManager.Parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class RefreshPodcastBatcher(
    private val batchSize: Int,
) {
    init {
        require(batchSize >= 1) { "Batch size must be positive: $batchSize" }
    }

    suspend fun refreshPodcasts(
        podcasts: List<Podcast>,
        sendBatch: suspend (Parameters) -> RefreshResponse?,
    ): Result<RefreshResponse?> {
        if (podcasts.isEmpty()) {
            return Result.success(null)
        }

        val batches = createBatches(podcasts)

        return try {
            val responses = sendAllBatches(batches, sendBatch).filterNotNull()
            Result.success(responses.reduceOrNull(RefreshResponse::merge))
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            Result.failure(e)
        }
    }

    private fun createBatches(podcasts: List<Podcast>) = podcasts.chunked(batchSize).map { chunk ->
        val podcastIds = StringBuilder()
        val episodeIds = StringBuilder()
        chunk.forEachIndexed { index, podcast ->
            if (index > 0) {
                podcastIds.append(LIST_SEPARATOR)
                episodeIds.append(LIST_SEPARATOR)
            }
            podcastIds.append(podcast.uuid)
            episodeIds.append(podcast.latestEpisodeUuid)
        }

        Parameters()
            .add("podcasts", podcastIds.toString())
            .add("last_episodes", episodeIds.toString())
            .add("push_on", "false")
    }

    private suspend fun sendAllBatches(
        batches: List<Parameters>,
        sendBatch: suspend (Parameters) -> RefreshResponse?,
    ) = coroutineScope {
        batches.map { async { sendBatch(it) } }.awaitAll()
    }

    private companion object {
        const val LIST_SEPARATOR = ","
    }
}
