package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDrawer
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.MediaItemCompatConverter
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.PackageValidator
import au.com.shiftyjelly.pocketcasts.utils.SchedulerProvider
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
open class LegacyPlaybackService :
    MediaBrowserServiceCompat(),
    CoroutineScope {

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var sleepTimer: SleepTimer

    @Inject lateinit var notificationDrawer: NotificationDrawer

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var eventHorizon: com.automattic.eventhorizon.EventHorizon

    @Inject lateinit var browseTreeProvider: BrowseTreeProvider

    private var mediaController: MediaControllerCompat? = null
        set(value) {
            field = value
            if (value != null) {
                val callback = MediaControllerCallback(value.metadata)
                value.registerCallback(callback)
                mediaControllerCallback = callback
            }
        }

    private var mediaControllerCallback: MediaControllerCallback? = null
    lateinit var notificationManager: PlayerNotificationManager

    private val disposables = CompositeDisposable()

    @Volatile
    private var isForeground: Boolean = false

    private var sleepTimerHandler: SleepTimerHandler? = null

    private var packageValidator: PackageValidator? = null

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    override fun onCreate() {
        super.onCreate()

        // Guard against the system starting this service from a stale component cache
        // when the Media3 flag is actually ON. In that case mediaSession is null.
        val mediaSession = playbackManager.mediaSessionManager.mediaSession
        if (mediaSession == null) {
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, "LegacyPlaybackService created but mediaSession is null (flag mismatch), stopping")
            stopSelf()
            return
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Legacy playback service created")

        if (!BuildConfig.DEBUG) {
            packageValidator = PackageValidator(this, LR.xml.allowed_media_browser_callers)
        }

        sessionToken = mediaSession.sessionToken
        mediaController = MediaControllerCompat(this, mediaSession.sessionToken)
        notificationManager = PlayerNotificationManagerImpl(this)

        sleepTimerHandler = SleepTimerHandler(sleepTimer, playbackManager, { applicationContext }, this).also { it.observe() }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (Util.isAutomotive(this)) return
        @Suppress("SENSELESS_COMPARISON") // mediaSession becomes nullable in the Media3 integration PR
        val session: MediaSessionCompat? = playbackManager.mediaSessionManager.mediaSession
        val state = session?.controller?.playbackState?.state
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_BUFFERING
        if (!isPlaying) {
            playbackManager.pause(sourceView = SourceView.AUTO_PAUSE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isForeground = false

        disposables.clear()
        sleepTimerHandler?.dispose()
        job.cancel()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Legacy playback service destroyed")
    }

    fun isForegroundService(): Boolean {
        return isForeground
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        val validator = packageValidator
        if (validator != null && !validator.isKnownCaller(clientPackageName, clientUid)) {
            Timber.e("Unknown caller: $clientPackageName $clientUid")
            return null
        }

        if (!clientPackageName.contains("au.com.shiftyjelly.pocketcasts")) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Client: $clientPackageName connected to legacy media session")
            if (Util.isAutomotive(this) && !settings.automotiveConnectedToMediaSession()) {
                launch {
                    kotlinx.coroutines.delay(1000)
                    settings.setAutomotiveConnectedToMediaSession(true)
                }
            }
        }

        val isRecent = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        val isSuggested = rootHints?.getBoolean(BrowserRoot.EXTRA_SUGGESTED) ?: false
        val hasCurrentEpisode = playbackManager.getCurrentEpisode() != null

        val rootId = browseTreeProvider.getRootId(isRecent, isSuggested, hasCurrentEpisode)
            ?: return null

        val extras = Bundle().apply {
            putBoolean(MEDIA_SEARCH_SUPPORTED, true)
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
        }
        return BrowserRoot(rootId, extras)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        launch {
            try {
                val items = browseTreeProvider.loadChildren(parentId, this@LegacyPlaybackService)
                result.sendResult(MediaItemCompatConverter.toCompatList(items))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load children for $parentId")
                result.sendResult(emptyList())
            }
        }
    }

    override fun onSearch(query: String, extras: Bundle?, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        launch {
            try {
                val items = browseTreeProvider.search(query, this@LegacyPlaybackService)
                result.sendResult(items?.let { MediaItemCompatConverter.toCompatList(it) } ?: emptyList())
            } catch (e: Exception) {
                Timber.e(e, "Search failed for: $query")
                result.sendResult(emptyList())
            }
        }
    }

    private inner class MediaControllerCallback(currentMetadataCompat: MediaMetadataCompat?) : MediaControllerCompat.Callback() {
        private val playbackStatusRelay = BehaviorRelay.create<PlaybackStateCompat>()
        private val mediaMetadataRelay = BehaviorRelay.create<MediaMetadataCompat>().apply {
            if (currentMetadataCompat != null) {
                accept(currentMetadataCompat)
            }
        }
        private val artworkConfiguration = settings.artworkConfiguration.flow.asObservable()

        init {
            Observables.combineLatest(playbackStatusRelay, mediaMetadataRelay, artworkConfiguration)
                .observeOn(SchedulerProvider.io)
                .distinctUntilChanged { (state1, metadata1, artworkConfiguration1), (state2, metadata2, artworkConfiguration2) ->
                    val isForegroundService = isForegroundService()
                    (state1.state == state2.state && metadata1.getString(METADATA_KEY_MEDIA_ID) == metadata2.getString(METADATA_KEY_MEDIA_ID) && artworkConfiguration1 == artworkConfiguration2) &&
                        (isForegroundService && (state2.state == PlaybackStateCompat.STATE_PLAYING || state2.state == PlaybackStateCompat.STATE_BUFFERING))
                }
                .map { (playbackState, _, artworkConfiguration) -> playbackState to buildNotification(artworkConfiguration.useEpisodeArtwork) }
                .observeOn(SchedulerProvider.mainThread)
                .subscribeBy(
                    onNext = { (state: PlaybackStateCompat, notification: Notification?) ->
                        onPlaybackStateChangedWithNotification(state, notification)
                    },
                    onError = { throwable ->
                        Timber.e(throwable)
                        LogBuffer.e(LogBuffer.TAG_PLAYBACK, throwable, "Legacy playback service error")
                    },
                )
                .addTo(disposables)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata ?: return
            mediaMetadataRelay.accept(metadata)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>) {
            Timber.i("Queue changed ${queue.size}. $queue")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state ?: return
            playbackStatusRelay.accept(state)
        }

        private fun onPlaybackStateChangedWithNotification(playbackState: PlaybackStateCompat, notification: Notification?) {
            val isForegroundService = isForegroundService()
            val state = playbackState.state

            if (notification != null && notificationHelper.isShowing(Settings.NotificationId.PLAYING.value)) {
                Timber.d("Updating playback notification")
                notificationManager.notify(Settings.NotificationId.PLAYING.value, notification)
                if (isForegroundService && (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_BUFFERING)) {
                    return
                }
            }

            Timber.d("Playback Notification State Change $state")
            when (state) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING,
                -> {
                    if (notification != null) {
                        try {
                            startForeground(Settings.NotificationId.PLAYING.value, notification)
                            isForeground = true
                            notificationManager.enteredForeground(notification)
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "startForeground state: $state")
                        } catch (e: Exception) {
                            LogBuffer.e(LogBuffer.TAG_PLAYBACK, "attempted startForeground for state: $state, but that threw an exception we caught: $e")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                                addBatteryWarnings()
                                eventHorizon.track(com.automattic.eventhorizon.PlaybackForegroundServiceErrorEvent)
                            }
                        }
                    } else {
                        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "can't startForeground as the notification is null")
                    }
                }

                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.STATE_ERROR,
                -> {
                    val removeNotification = state != PlaybackStateCompat.STATE_PAUSED || settings.hideNotificationOnPause.value
                    if (removeNotification || isForegroundService) {
                        val isTransientLoss = playbackManager.playbackStateRelay.blockingFirst().transientLoss
                        if (isTransientLoss) {
                            return
                        }

                        if (notification != null && state == PlaybackStateCompat.STATE_PAUSED && isForegroundService) {
                            notificationManager.notify(Settings.NotificationId.PLAYING.value, notification)
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "stopForeground state: $state (update notification)")
                        } else {
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "stopForeground state: $state removing notification: $removeNotification")
                        }

                        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
                        isForeground = false
                        if (removeNotification) {
                            notificationManager.cancel(Settings.NotificationId.PLAYING.value)
                        }
                    }

                    if (state == PlaybackStateCompat.STATE_ERROR) {
                        LogBuffer.e(
                            LogBuffer.TAG_PLAYBACK,
                            "Playback state error: ${playbackStatusRelay.value?.errorCode ?: -1} ${playbackStatusRelay.value?.errorMessage ?: "Unknown error"}",
                        )
                    }
                }
            }
        }

        private fun addBatteryWarnings() {
            val currentValue = settings.getTimesToShowBatteryWarning()
            settings.setTimesToShowBatteryWarning(2 + currentValue)
        }

        private fun buildNotification(useEpisodeArtwork: Boolean): Notification? {
            if (Util.isAutomotive(this@LegacyPlaybackService)) return null
            val mediaSession = playbackManager.mediaSessionManager.mediaSession ?: return null
            return notificationDrawer.buildPlayingNotification(mediaSession.sessionToken, useEpisodeArtwork)
        }
    }
}
