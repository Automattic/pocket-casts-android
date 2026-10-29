package au.com.shiftyjelly.pocketcasts.repositories.chat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ProcessLifecycle
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@Singleton
class ChatStarterQuestionsPrefetcher @Inject constructor(
    private val upNextQueue: UpNextQueue,
    private val transcriptManager: TranscriptManager,
    private val chatManager: ChatManager,
    private val syncManager: SyncManager,
    private val settings: Settings,
    @ProcessLifecycle private val processLifecycleOwner: LifecycleOwner,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val isPrefetching = AtomicBoolean()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun prefetchForCurrentEpisode() {
        if (isPrefetching.getAndSet(true)) return
        scope.launch {
            combine(currentEpisode(), canChat(), isAppForeground()) { episode, canChat, isAppForeground ->
                episode.takeIf { canChat && isAppForeground }
            }
                .distinctUntilChangedBy { it?.uuid }
                .flatMapLatest { episode ->
                    if (episode == null) emptyFlow() else generatedTranscriptAvailable(episode)
                }
                .catch { LogBuffer.e(LOG_TAG, it, "Failed to observe the episode for starter questions") }
                .collectLatest { episode -> prefetch(episode) }
        }
    }

    private fun generatedTranscriptAvailable(episode: PodcastEpisode) = flow {
        delay(CURRENT_EPISODE_DELAY)
        emitAll(
            transcriptManager.observeIsGeneratedTranscriptAvailable(episode.uuid)
                .filter { it }
                .take(1)
                .map { episode },
        )
    }

    private suspend fun prefetch(episode: PodcastEpisode) {
        try {
            if (!chatManager.hasChatWithMessages(episode.uuid)) {
                chatManager.getStarterQuestions(episode.uuid, episode.podcastUuid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LOG_TAG, e, "Failed to prefetch starter questions")
        }
    }

    private fun currentEpisode(): Flow<PodcastEpisode?> = upNextQueue.changesFlow.map { state ->
        (state as? UpNextQueue.State.Loaded)?.episode as? PodcastEpisode
    }

    private fun canChat(): Flow<Boolean> = combine(
        FeatureFlag.isEnabledFlow(Feature.EPISODE_CHAT),
        FeatureFlag.isEnabledFlow(Feature.EPISODE_CHAT_FREE_BETA),
        settings.cachedSubscription.flow,
        syncManager.isLoggedInObservable.asFlow(),
    ) { isChatEnabled, isFreeBetaEnabled, subscription, isLoggedIn ->
        isChatEnabled && isLoggedIn && (subscription != null || isFreeBetaEnabled)
    }

    private fun isAppForeground(): Flow<Boolean> = processLifecycleOwner.lifecycle.currentStateFlow.map { state ->
        state.isAtLeast(Lifecycle.State.STARTED)
    }

    companion object {
        val CURRENT_EPISODE_DELAY = 3.seconds
        private const val LOG_TAG = "EpisodeChat"
    }
}
