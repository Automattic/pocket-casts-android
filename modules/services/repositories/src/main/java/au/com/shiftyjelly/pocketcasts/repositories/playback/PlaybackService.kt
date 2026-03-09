package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR

const val MEDIA_ID_ROOT = "__ROOT__"
const val PODCASTS_ROOT = "__PODCASTS__"
const val RECENT_ROOT = "__RECENT__"
const val SUGGESTED_ROOT = "__SUGGESTED__"
const val FOLDER_ROOT_PREFIX = "__FOLDER__"

internal const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
internal const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"

const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
const val EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT = "android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT"

/**
 * Value for {@link #CONTENT_STYLE_PLAYABLE_HINT} and {@link #CONTENT_STYLE_BROWSABLE_HINT} that
 * hints the corresponding items should be presented as lists.  */
const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1

/**
 * Value for {@link #CONTENT_STYLE_PLAYABLE_HINT} and {@link #CONTENT_STYLE_BROWSABLE_HINT} that
 * hints the corresponding items should be presented as grids.  */
const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2

@AndroidEntryPoint
open class PlaybackService :
    MediaLibraryService(),
    CoroutineScope {
    inner class LocalBinder : Binder() {
        val service: PlaybackService
            get() = this@PlaybackService
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return playbackManager.mediaSessionManager.getMedia3Session()
    }

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var sleepTimer: SleepTimer

    private var sleepTimerDisposable: Disposable? = null
    private var currentTimeLeft: Duration = ZERO

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onBind(intent: Intent?): IBinder? {
        val binder = super.onBind(intent)
        return binder ?: LocalBinder()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Playback service created")

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK.id)
            .setNotificationId(Settings.NotificationId.PLAYING.value)
            .build()
        notificationProvider.setSmallIcon(IR.drawable.notification)
        setMediaNotificationProvider(notificationProvider)

        playbackManager.mediaSessionManager.createSession(this)

        // Register the session with the service so the internal MediaNotificationManager
        // starts monitoring player state and posting notifications immediately,
        // rather than waiting for an external controller to bind.
        playbackManager.mediaSessionManager.getMedia3Session()?.let { addSession(it) }

        observePlaybackState()
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val isTransientLoss = (session.player as? PocketCastsForwardingPlayer)?.isTransientLoss == true
        if (!startInForegroundRequired && settings.hideNotificationOnPause.value && !isTransientLoss) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playbackManager.mediaSessionManager.getMedia3Session()?.player
        if (player == null || !player.playWhenReady) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        playbackManager.mediaSessionManager.release()
        super.onDestroy()

        sleepTimerDisposable?.dispose()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Playback service destroyed")
    }

    private fun observePlaybackState() {
        sleepTimer.stateFlow
            .map { it }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .onEach { state ->
                onSleepTimerStateChange(state)
            }
            .catch { throwable ->
                Timber.e(throwable, "Error observing SleepTimer state")
            }
            .launchIn(this)
    }

    private fun onSleepTimerStateChange(state: SleepTimerState) {
        if (state.isSleepTimerRunning && state.timeLeft != ZERO) {
            startOrUpdateSleepTimer(state.timeLeft)
        } else {
            cancelSleepTimer()
        }
    }

    private fun startOrUpdateSleepTimer(newTimeLeft: Duration) {
        if (newTimeLeft == ZERO || newTimeLeft.isNegative()) {
            return
        }

        if (sleepTimerDisposable == null || sleepTimerDisposable!!.isDisposed) {
            currentTimeLeft = newTimeLeft

            sleepTimerDisposable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
                .takeWhile { currentTimeLeft > ZERO }
                .doOnNext {
                    currentTimeLeft = currentTimeLeft.minus(1.seconds)
                    sleepTimer.updateSleepTimerStatus(sleepTimeRunning = currentTimeLeft != ZERO, timeLeft = currentTimeLeft)

                    if (currentTimeLeft == 5.seconds) {
                        playbackManager.performVolumeFadeOut(5.0)
                    }

                    if (currentTimeLeft <= ZERO) {
                        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused from sleep timer.")
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(applicationContext, applicationContext.getString(R.string.player_sleep_timer_stopped_your_podcast), Toast.LENGTH_LONG).show()
                            playbackManager.restorePlayerVolume()
                        }
                        playbackManager.pause(sourceView = SourceView.AUTO_PAUSE)
                        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
                        cancelSleepTimer()
                    }
                }
                .subscribe()
        } else {
            currentTimeLeft = newTimeLeft
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerDisposable?.dispose()
        sleepTimerDisposable = null
        currentTimeLeft = ZERO
    }
}
