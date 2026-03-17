package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.extensions.id
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDrawer
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.MediaItemCompatConverter
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.PackageValidator
import au.com.shiftyjelly.pocketcasts.utils.IS_RUNNING_UNDER_TEST
import au.com.shiftyjelly.pocketcasts.utils.SchedulerProvider
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlaybackForegroundServiceErrorEvent
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.Timer
import java.util.TimerTask
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
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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

/**
 * Value for {​@link ​#CONTENT_STYLE_PLAYABLE_HINT} and {​@link #CONTENT_STYLE_BROWSABLE_HINT} that
 * hints the corresponding items should be presented as lists.  */
const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1

/**
 * Value for {​@link ​#CONTENT_STYLE_PLAYABLE_HINT} and {​@link #CONTENT_STYLE_BROWSABLE_HINT} that
 * hints the corresponding items should be presented as grids.  */
const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2

@AndroidEntryPoint
open class PlaybackService :
    MediaBrowserServiceCompat(),
    CoroutineScope {
    inner class LocalBinder : Binder() {
        val service: PlaybackService
            get() = this@PlaybackService
    }

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var notificationDrawer: NotificationDrawer

    @Inject lateinit var settings: Settings

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var eventHorizon: EventHorizon

    @Inject lateinit var sleepTimer: SleepTimer

    @Inject lateinit var browseTreeProvider: BrowseTreeProvider

    var mediaController: MediaControllerCompat? = null
        set(value) {
            field = value
            if (value != null) {
                val mediaControllerCallback = MediaControllerCallback(value.metadata)
                value.registerCallback(mediaControllerCallback)
                this.mediaControllerCallback = mediaControllerCallback
            }
        }

    private var mediaControllerCallback: MediaControllerCallback? = null
    lateinit var notificationManager: PlayerNotificationManager

    private val disposables = CompositeDisposable()

    @Volatile
    private var isForeground: Boolean = false
    private var sleepTimerDisposable: Disposable? = null
    private var currentTimeLeft: Duration = ZERO

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onBind(intent: Intent?): IBinder? {
        val binder = super.onBind(intent)
        return binder ?: LocalBinder() // We return our local binder for tests and use the media session service binder normally
    }

    override fun onCreate() {
        super.onCreate()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Playback service created")

        val mediaSession = playbackManager.mediaSession
        sessionToken = mediaSession.sessionToken

        mediaController = MediaControllerCompat(this, mediaSession)
        notificationManager = PlayerNotificationManagerImpl(this)

        observePlaybackState()
    }

    override fun onDestroy() {
        super.onDestroy()
        isForeground = false

        disposables.clear()
        sleepTimerDisposable?.dispose()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Playback service destroyed")
    }

    fun isForegroundService(): Boolean {
        return isForeground
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
                // only generate new notifications for a different playback state and episode. Also if we are playing but aren't a foreground service something isn't right
                .distinctUntilChanged { (state1, metadata1, artworkConfiguration1), (state2, metadata2, artworkConfiguration2) ->
                    val isForegroundService = isForegroundService()
                    (state1.state == state2.state && metadata1.id == metadata2.id && artworkConfiguration1 == artworkConfiguration2) &&
                        (isForegroundService && (state2.state == PlaybackStateCompat.STATE_PLAYING || state2.state == PlaybackStateCompat.STATE_BUFFERING))
                }
                // build the notification including artwork in the background
                .map { (playbackState, metadata, artworkConfiguration) -> playbackState to buildNotification(playbackState.state, metadata, artworkConfiguration.useEpisodeArtwork) }
                .observeOn(SchedulerProvider.mainThread)
                .subscribeBy(
                    onNext = { (state: PlaybackStateCompat, notification: Notification?) ->
                        onPlaybackStateChangedWithNotification(state, notification)
                    },
                    onError = { throwable ->
                        Timber.e(throwable)
                        LogBuffer.e(LogBuffer.TAG_PLAYBACK, throwable, "Playback service error")
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

        /***
         // This is the most fragile and important code in the app, edit with care
         // Possible bugs to watch out for are:
         // - No notification shown during playback which means no foregrounds service, app could be killed or stutter
         // - Notification coming back after pausing
         // - Incorrect state shown in notification compared with player
         // - Notification not being able to be dismissed after pausing playback
         ***/
        private fun onPlaybackStateChangedWithNotification(playbackState: PlaybackStateCompat, notification: Notification?) {
            val isForegroundService = isForegroundService()
            val state = playbackState.state

            // If we are already showing a notification, update it no matter the state.
            if (notification != null && notificationHelper.isShowing(Settings.NotificationId.PLAYING.value)) {
                Timber.d("Updating playback notification")
                notificationManager.notify(Settings.NotificationId.PLAYING.value, notification)
                if (isForegroundService && (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_BUFFERING)) {
                    // Nothing else to do
                    return
                }
            }

            Timber.d("Playback Notification State Change $state")
            // Transition between foreground service running and not with a notification
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
                                eventHorizon.track(PlaybackForegroundServiceErrorEvent)
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
                    // We have to be careful here to only call notify when moving from PLAY to PAUSE once
                    // or else the notification will come back after being swiped away
                    if (removeNotification || isForegroundService) {
                        val isTransientLoss = playbackState.extras?.getBoolean(MediaSessionManager.EXTRA_TRANSIENT) ?: false
                        if (isTransientLoss) {
                            // Don't kill the foreground service for transient pauses
                            return
                        }

                        if (notification != null && state == PlaybackStateCompat.STATE_PAUSED && isForegroundService) {
                            notificationManager.notify(Settings.NotificationId.PLAYING.value, notification)
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "stopForeground state: $state (update notification)")
                        } else {
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "stopForeground state: $state removing notification: $removeNotification")
                        }

                        // When paused keep the notification otherwise remove it
                        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
                        isForeground = false
                        if (removeNotification) {
                            notificationManager.cancel(Settings.NotificationId.PLAYING.value)
                        }
                    }

                    if (state == PlaybackStateCompat.STATE_ERROR) {
                        LogBuffer.e(
                            LogBuffer.TAG_PLAYBACK,
                            "Playback state error: ${playbackStatusRelay.value?.errorCode
                                ?: -1} ${playbackStatusRelay.value?.errorMessage
                                ?: "Unknown error"}",
                        )
                    }
                }
            }
        }

        private fun addBatteryWarnings() {
            val currentValue = settings.getTimesToShowBatteryWarning()
            settings.setTimesToShowBatteryWarning(2 + currentValue)
        }

        private fun buildNotification(
            state: Int,
            metadata: MediaMetadataCompat?,
            useEpisodeArtwork: Boolean,
        ): Notification? {
            if (Util.isAutomotive(this@PlaybackService)) {
                return null
            }

            val sessionToken = sessionToken
            if (metadata == null || metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).isEmpty()) return null
            return if (state != PlaybackStateCompat.STATE_NONE && sessionToken != null) notificationDrawer.buildPlayingNotification(sessionToken, useEpisodeArtwork) else null
        }
    }

    /****
     * testPlaybackStateChange
     * This method can be used for tests to pass in a playback state change to pass through.
     * Ideally we could mock mediacontroller and notificationcontroller but mocking final classes
     * is not supported on Android
     * @param metadata Metadata for playback
     * @param playbackStateCompat Playback state to pass through the service
     */
    fun testPlaybackStateChange(metadata: MediaMetadataCompat?, playbackStateCompat: PlaybackStateCompat) {
        assert(IS_RUNNING_UNDER_TEST) // This method should only be used for testing
        mediaControllerCallback?.onMetadataChanged(metadata)
        mediaControllerCallback?.onPlaybackStateChanged(playbackStateCompat)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, bundle: Bundle?): BrowserRoot? {
        val extras = Bundle()

        Timber.d("onGetRoot() $clientPackageName ${bundle?.keySet()?.toList()}")
        // tell Android Auto we support media search
        extras.putBoolean(MEDIA_SEARCH_SUPPORTED, true)

        // tell Android Auto we support grids and lists and that browsable things should be grids, the rest lists
        extras.putBoolean(CONTENT_STYLE_SUPPORTED, true)
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)

        // To ensure you are not allowing any arbitrary app to browse your app's contents, check the origin
        if (!PackageValidator(this, LR.xml.allowed_media_browser_callers).isKnownCaller(clientPackageName, clientUid) && !BuildConfig.DEBUG) {
            // If the request comes from an untrusted package, return null
            Timber.e("Unknown caller trying to connect to media service $clientPackageName $clientUid")
            return null
        }

        if (!clientPackageName.contains("au.com.shiftyjelly.pocketcasts")) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Client: $clientPackageName connected to media session") // Log things like Android Auto or Assistant connecting
            if (Util.isAutomotive(applicationContext) && !settings.automotiveConnectedToMediaSession()) {
                Timer().schedule(
                    object : TimerTask() {
                        override fun run() {
                            settings.setAutomotiveConnectedToMediaSession(true)
                        }
                    },
                    1000,
                )
            }
        }

        val rootId = browseTreeProvider.getRootId(
            isRecent = browserRootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) == true,
            isSuggested = browserRootHints?.getBoolean(BrowserRoot.EXTRA_SUGGESTED) == true,
            hasCurrentEpisode = playbackManager.getCurrentEpisode() != null,
        )
        return if (rootId != null) BrowserRoot(rootId, extras) else null
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        launch {
            val items = browseTreeProvider.loadChildren(parentId, this@PlaybackService)
            result.sendResult(MediaItemCompatConverter.toCompatList(items))
        }
    }

    override fun onSearch(query: String, extras: Bundle?, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        launch {
            val items = browseTreeProvider.search(query, this@PlaybackService)
            result.sendResult(items?.let { MediaItemCompatConverter.toCompatList(it) })
        }
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
