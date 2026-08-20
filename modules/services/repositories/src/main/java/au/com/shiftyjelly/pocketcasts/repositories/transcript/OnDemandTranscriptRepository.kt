package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager
import com.pocketcasts.service.api.OnDemandTranscriptEnablement
import com.pocketcasts.service.api.OnDemandTranscriptOutcome
import com.pocketcasts.service.api.OnDemandTranscriptReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

interface OnDemandTranscriptRepository {
    suspend fun request(podcastUuid: String, episodeUuid: String): RequestResult

    suspend fun refreshMetadata(podcastUuid: String, episodeUuid: String)

    data class RequestResult(
        val outcome: Outcome,
        val reason: String,
        val enablement: String,
        val newlyQueuedCount: Int,
    )

    enum class Outcome {
        Queued,
        InProgress,
        Available,
        NotEligible,
        TransientFailure,
        Throttled,
        Unknown,
    }
}

@Singleton
class OnDemandTranscriptRepositoryImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val syncServiceManager: SyncServiceManager,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
    private val episodeManager: EpisodeManager,
    private val showNotesManager: ShowNotesManager,
) : OnDemandTranscriptRepository {
    override suspend fun request(
        podcastUuid: String,
        episodeUuid: String,
    ): OnDemandTranscriptRepository.RequestResult {
        return try {
            val response = syncManager.getCacheTokenOrLogin { token ->
                syncServiceManager.requestOnDemandTranscript(
                    podcastUuid = podcastUuid,
                    episodeUuid = episodeUuid,
                    token = token,
                )
            }
            OnDemandTranscriptRepository.RequestResult(
                outcome = response.outcome.toDomain(),
                reason = response.reason.analyticsValue,
                enablement = response.enablement.analyticsValue,
                newlyQueuedCount = response.newlyQueuedCount,
            )
        } catch (error: HttpException) {
            OnDemandTranscriptRepository.RequestResult(
                outcome = error.code().toOnDemandTranscriptOutcome(),
                reason = "http_${error.code()}",
                enablement = "unspecified",
                newlyQueuedCount = 0,
            )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            OnDemandTranscriptRepository.RequestResult(
                outcome = OnDemandTranscriptRepository.Outcome.TransientFailure,
                reason = "network_error",
                enablement = "unspecified",
                newlyQueuedCount = 0,
            )
        }
    }

    override suspend fun refreshMetadata(
        podcastUuid: String,
        episodeUuid: String,
    ) {
        val remotePodcast = podcastCacheServiceManager.getPodcastAndEpisode(podcastUuid, episodeUuid)
        val remoteEpisode = remotePodcast.episodes.firstOrNull { it.uuid == episodeUuid }
        val localEpisode = episodeManager.findByUuid(episodeUuid)
        if (remoteEpisode != null && localEpisode != null) {
            localEpisode.hasGeneratedTranscript = remoteEpisode.hasGeneratedTranscript
            episodeManager.update(localEpisode)
        }
        showNotesManager.refreshTranscriptMetadata(podcastUuid, episodeUuid)
    }
}

internal fun Int.toOnDemandTranscriptOutcome() = when (this) {
    400, 401, 403, 404 -> OnDemandTranscriptRepository.Outcome.NotEligible
    429 -> OnDemandTranscriptRepository.Outcome.Throttled
    in 500..599 -> OnDemandTranscriptRepository.Outcome.TransientFailure
    else -> OnDemandTranscriptRepository.Outcome.Unknown
}

private fun OnDemandTranscriptOutcome.toDomain() = when (this) {
    OnDemandTranscriptOutcome.QUEUED -> OnDemandTranscriptRepository.Outcome.Queued

    OnDemandTranscriptOutcome.IN_PROGRESS -> OnDemandTranscriptRepository.Outcome.InProgress

    OnDemandTranscriptOutcome.AVAILABLE -> OnDemandTranscriptRepository.Outcome.Available

    OnDemandTranscriptOutcome.NOT_ELIGIBLE -> OnDemandTranscriptRepository.Outcome.NotEligible

    OnDemandTranscriptOutcome.TRANSIENT_FAILURE -> OnDemandTranscriptRepository.Outcome.TransientFailure

    OnDemandTranscriptOutcome.THROTTLED -> OnDemandTranscriptRepository.Outcome.Throttled

    OnDemandTranscriptOutcome.OUTCOME_UNSPECIFIED,
    OnDemandTranscriptOutcome.UNRECOGNIZED,
    -> OnDemandTranscriptRepository.Outcome.Unknown
}

private val OnDemandTranscriptReason.analyticsValue
    get() = name.lowercase()

private val OnDemandTranscriptEnablement.analyticsValue
    get() = name.lowercase()
