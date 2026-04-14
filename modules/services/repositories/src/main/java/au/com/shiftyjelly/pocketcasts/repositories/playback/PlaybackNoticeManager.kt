package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
    val supportUrl: String? = null,
    val linkText: String? = null,
)

@Singleton
class PlaybackNoticeManager @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    private val appLifecycleProvider: AppLifecycleProvider,
    private val errorClassifier: PlaybackErrorClassifier,
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
        var hasReceivedCapabilities = false
        var recoveryDismissJob: Job? = null
        var connectionLostDismissJob: Job? = null

        applicationScope.launch {
            networkConnectionWatcher.networkCapabilities.collect { capabilities ->
                val isOffline = capabilities == null ||
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                if (isOffline && capabilities == null && !hasReceivedCapabilities) {
                    hasReceivedCapabilities = true
                    return@collect
                }
                hasReceivedCapabilities = true

                if (isOffline) {
                    recoveryDismissJob?.cancel()
                    connectionLostDismissJob?.cancel()
                    connectionNotice.value = PlaybackNoticeInfo(
                        message = context.getString(LR.string.error_playback_offline),
                        type = PlaybackNoticeType.CONNECTION_LOST,
                    )
                    connectionLostDismissJob = launch {
                        delay(AUTO_DISMISS_DURATION)
                        connectionNotice.value = null
                    }
                    wasOffline = true
                } else if (wasOffline) {
                    wasOffline = false
                    connectionLostDismissJob?.cancel()
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

                    playbackState.transientLoss -> null

                    !isForeground -> null

                    playbackState.playbackIssue is PlaybackIssue.ConnectionError -> PlaybackNoticeInfo(
                        message = context.getString(LR.string.error_playback_offline),
                        type = PlaybackNoticeType.CONNECTION_LOST,
                    )

                    playbackState.playbackIssue is PlaybackIssue.StuckPlayer -> PlaybackNoticeInfo(
                        message = context.getString(LR.string.error_streaming_access_denied),
                        type = PlaybackNoticeType.PLAYBACK,
                        supportUrl = Settings.INFO_DOWNLOAD_AND_PLAYBACK_URL,
                        linkText = context.getString(LR.string.settings_battery_learn_more),
                    )

                    else -> {
                        val httpCode = (playbackState.playbackIssue as? PlaybackIssue.HttpError)?.statusCode
                        PlaybackNoticeInfo(
                            message = context.getString(LR.string.error_streaming_access_denied),
                            type = PlaybackNoticeType.PLAYBACK,
                            supportUrl = errorClassifier.classifyHelpUrl(httpCode),
                            linkText = context.getString(LR.string.settings_battery_learn_more),
                        )
                    }
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
