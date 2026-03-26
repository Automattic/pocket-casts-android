package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class PlaybackNoticeType {
    CONNECTION_LOST,
    PLAYBACK,
    RECOVERY,
}

data class PlaybackNoticeInfo(
    val message: String,
    val type: PlaybackNoticeType,
)

@Singleton
class PlaybackNoticeManager @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    private val appLifecycleProvider: AppLifecycleProvider,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    private val connectionNotice = MutableStateFlow<PlaybackNoticeInfo?>(null)
    private val playbackErrorNotice = MutableStateFlow<PlaybackNoticeInfo?>(null)

    init {
        monitorConnectionIssues()
        monitorPlaybackIssues()
    }

    val playbackNotice: Flow<PlaybackNoticeInfo?> = combine(
        connectionNotice,
        playbackErrorNotice,
        FeatureFlag.isEnabledFlow(Feature.PLAYBACK_ERROR_INFO_BAR),
    ) { conn, playback, enabled ->
        if (!enabled) null else conn ?: playback
    }.distinctUntilChanged()

    private fun monitorConnectionIssues() {
        var wasOffline = false
        var isFirstEmission = true
        var recoveryDismissJob: Job? = null

        applicationScope.launch {
            networkConnectionWatcher.networkCapabilities.collect { capabilities ->
                val isOffline = capabilities == null ||
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                if (isOffline) {
                    recoveryDismissJob?.cancel()
                    connectionNotice.value = PlaybackNoticeInfo(
                        message = context.getString(LR.string.error_playback_offline),
                        type = PlaybackNoticeType.CONNECTION_LOST,
                    )
                    if (!isFirstEmission) wasOffline = true
                } else if (wasOffline) {
                    wasOffline = false
                    connectionNotice.value = PlaybackNoticeInfo(
                        message = context.getString(LR.string.error_playback_connected),
                        type = PlaybackNoticeType.RECOVERY,
                    )
                    recoveryDismissJob = launch {
                        delay(AUTO_DISMISS_DURATION)
                        connectionNotice.value = null
                    }
                } else if (recoveryDismissJob?.isActive != true) {
                    connectionNotice.value = null
                }
                isFirstEmission = false
            }
        }
    }

    private fun monitorPlaybackIssues() {
        var autoDismissJob: Job? = null

        applicationScope.launch {
            combine(
                playbackManager.playbackStateFlow,
                appLifecycleProvider.isInForeground,
            ) { playbackState, isForeground ->
                when {
                    !playbackState.isError -> null

                    isForeground -> PlaybackNoticeInfo(
                        message = context.getString(LR.string.error_episode_not_available),
                        type = PlaybackNoticeType.PLAYBACK,
                    )

                    else -> null
                }
            }.collect { notice ->
                autoDismissJob?.cancel()
                playbackErrorNotice.value = notice
                if (notice != null) {
                    autoDismissJob = launch {
                        delay(AUTO_DISMISS_DURATION)
                        playbackErrorNotice.value = null
                    }
                }
            }
        }
    }

    companion object {
        val AUTO_DISMISS_DURATION = 5.seconds
    }
}
