package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlaybackForegroundServiceErrorEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import au.com.shiftyjelly.pocketcasts.images.R as IR

const val MEDIA_ID_ROOT = "__ROOT__"
const val PODCASTS_ROOT = "__PODCASTS__"
const val RECENT_ROOT = "__RECENT__"
const val SUGGESTED_ROOT = "__SUGGESTED__"
const val FOLDER_ROOT_PREFIX = "__FOLDER__"
const val UP_NEXT_ROOT = "__UP_NEXT__"

internal const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
internal const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"

const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
const val EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT = "android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT"

const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
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

    @Inject lateinit var eventHorizon: EventHorizon

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private var sleepTimerHandler: SleepTimerHandler? = null

    override fun onBind(intent: Intent?): IBinder? {
        val binder = super.onBind(intent)
        if (binder == null) {
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, "MediaLibraryService.onBind returned null for action: ${intent?.action}")
        }
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

        playbackManager.mediaSessionManager.getMedia3Session()?.let { addSession(it) }

        sleepTimerHandler = SleepTimerHandler(sleepTimer, playbackManager, { applicationContext }, this).also { it.observe() }
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // AAOS handles media UI entirely — skip app-managed notification removal
        // to avoid killing the foreground service.
        val isTransientLoss = (session.player as? PocketCastsForwardingPlayer)?.isTransientLoss == true
        if (!Util.isAutomotive(this) && !startInForegroundRequired && settings.hideNotificationOnPause.value && !isTransientLoss) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, "onUpdateNotification failed: $e")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                val currentValue = settings.getTimesToShowBatteryWarning()
                settings.setTimesToShowBatteryWarning(2 + currentValue)
                eventHorizon.track(PlaybackForegroundServiceErrorEvent)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // MediaLibraryService doesn't automatically route ACTION_MEDIA_BUTTON intents
        // sent via PendingIntent.getService (e.g., from PiP controls) to the session callback.
        // Forward them so onMediaButtonEvent is invoked.
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val session = playbackManager.mediaSessionManager.getMedia3Session()
            val callback = playbackManager.mediaSessionManager.media3Callback
            if (session != null && callback != null) {
                // Use the media notification controller if available, otherwise fall back to
                // any connected controller. If none exist, still attempt to handle the intent
                // by routing it through the callback — the controller info is only used for
                // logging/authorization, not for the actual media button handling.
                val controller = session.mediaNotificationControllerInfo
                    ?: session.connectedControllers.firstOrNull()
                if (controller != null) {
                    callback.onMediaButtonEvent(session, controller, intent)
                } else {
                    LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Media button event dropped: no controllers connected")
                }
                return START_NOT_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (Util.isAutomotive(this)) return
        val player = playbackManager.mediaSessionManager.getMedia3Session()?.player
        if (player == null || !player.playWhenReady) {
            // Pause via PlaybackManager first to ensure bookkeeping (position save,
            // state relay update) happens before the service stops.
            playbackManager.pause(sourceView = SourceView.AUTO_PAUSE)
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.mediaSessionManager.release()

        sleepTimerHandler?.dispose()
        job.cancel()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Playback service destroyed")
    }
}
