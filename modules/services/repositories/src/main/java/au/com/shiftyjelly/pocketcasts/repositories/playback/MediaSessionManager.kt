package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.preferences.model.LastPlayedList
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.getLaunchActivityPendingIntent
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR

class MediaSessionManager(
    val playbackManager: PlaybackManager,
    val podcastManager: PodcastManager,
    val episodeManager: EpisodeManager,
    val playlistManager: PlaylistManager,
    val settings: Settings,
    val context: Context,
    val episodeAnalytics: EpisodeAnalytics,
    val bookmarkManager: BookmarkManager,
) : CoroutineScope {
    companion object {
        const val EXTRA_TRANSIENT = "pocketcasts_transient_loss"
        const val ACTION_NOT_SUPPORTED = "action_not_supported"

        // there's an issue on Samsung phones that if you don't say you support ACTION_SKIP_TO_PREVIOUS and
        // ACTION_SKIP_TO_NEXT then the skip buttons on the lock screen are disabled. We work around this
        // by hiding our custom buttons on Samsung phones. It means the buttons in Android Auto aren't our
        // custom skip buttons, but it all still works.
        private val MANUFACTURERS_TO_HIDE_CUSTOM_SKIP_BUTTONS = listOf("samsung", "mercedes-benz")

        fun calculateSearchQueryOptions(query: String): List<String> {
            val options = mutableListOf<String>()
            options.add(query)
            val parts = query.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size > 1) {
                for (i in parts.size - 1 downTo 1) {
                    val lessParts = arrayOfNulls<String>(i)
                    System.arraycopy(parts, 0, lessParts, 0, i)
                    options.add(lessParts.joinToString(separator = " "))
                }
            }
            return options
        }
    }

    val mediaSession = MediaSessionCompat(context, "PocketCastsMediaSession")
    val disposables = CompositeDisposable()
    private val source = SourceView.MEDIA_BUTTON_BROADCAST_ACTION

    private var bookmarkHelper: BookmarkHelper

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    init {
        mediaSession.setCallback(MediaSessionCallback(playbackManager, episodeManager))

        if (!Util.isAutomotive(context)) { // We can't start activities on automotive
            mediaSession.setSessionActivity(context.getLaunchActivityPendingIntent())
        }
        mediaSession.setRatingType(RatingCompat.RATING_HEART)

        // this tells the session not to shuffle all our buttons over one when there's no playlist currently loaded. This keeps our skip buttons on either side of play/pause
        val extras = Bundle()
        extras.putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_QUEUE", true)
        mediaSession.setExtras(extras)

        bookmarkHelper = BookmarkHelper(
            playbackManager,
            bookmarkManager,
            settings
        )

        connect()
    }

    fun startObserving() {
        observePlaybackState()
        observeCustomMediaActionsVisibility()
        observeMediaNotificationControls()
        playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
            // ignore the playing episode progress updates, but update when the media player read the duration from the file.
            .distinctUntilChanged { stateOne, stateTwo ->
                UpNextQueue.State.isEqualWithEpisodeCompare(stateOne, stateTwo) { episodeOne, episodeTwo ->
                    episodeOne.uuid == episodeTwo.uuid && episodeOne.duration == episodeTwo.duration
                }
            }
            .observeOn(Schedulers.io())
            .doOnNext { updateUpNext(it) }
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    private fun observeCustomMediaActionsVisibility() {
        launch {
            settings.customMediaActionsVisibility.flow.collect {
                withContext(Dispatchers.Main) {
                    val playbackStateCompat = getPlaybackStateCompat(playbackManager.playbackStateRelay.blockingFirst(), currentEpisode = playbackManager.getCurrentEpisode())
                    // Called to update playback state with updated custom media actions visibility
                    updatePlaybackState(playbackStateCompat)
                }
            }
        }
    }

    private fun observeMediaNotificationControls() {
        launch {
            settings.mediaControlItems.flow.collect {
                withContext(Dispatchers.Main) {
                    val playbackStateCompat = getPlaybackStateCompat(playbackManager.playbackStateRelay.blockingFirst(), currentEpisode = playbackManager.getCurrentEpisode())
                    updatePlaybackState(playbackStateCompat)
                }
            }
        }
    }

    private fun connect() {
        // start the foreground service
        val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {}
        val mediaBrowser = MediaBrowserCompat(context, ComponentName(context, PlaybackService::class.java), connectionCallback, null)
        mediaBrowser.connect()
    }

    private fun getPlaybackStateRx(playbackState: PlaybackState, currentEpisode: Optional<BaseEpisode>): Single<PlaybackStateCompat> {
        return Single.fromCallable {
            getPlaybackStateCompat(playbackState, currentEpisode.get())
        }
    }

    private fun updatePlaybackState(playbackState: PlaybackStateCompat) {
        Timber.i("MediaSession playback state. $playbackState")
        mediaSession.setPlaybackState(playbackState)
    }

    private fun getPlaybackStateCompat(playbackState: PlaybackState, currentEpisode: BaseEpisode?): PlaybackStateCompat {
        if (playbackState.isError) {
            mediaSession.isActive = false
            return PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, playbackState.lastErrorMessage)
                .build()
        }

        if (playbackState.isPlaying || playbackState.transientLoss) {
            mediaSession.isActive = true
        }

        if (playbackState.isEmpty || currentEpisode == null) {
            val stateBuilder = PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
            return stateBuilder.build()
        }

        val state = if (playbackState.isPlaying) {
            if (playbackState.isBuffering) PlaybackStateCompat.STATE_BUFFERING else PlaybackStateCompat.STATE_PLAYING
        } else {
            if (playbackState.state == PlaybackState.State.STOPPED) PlaybackStateCompat.STATE_STOPPED else PlaybackStateCompat.STATE_PAUSED
        }

        val currentSpeed = playbackState.playbackSpeed
        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(state, playbackState.positionMs.toLong(), currentSpeed.toFloat(), SystemClock.elapsedRealtime())
            .setActions(getSupportedActions(playbackState))
            .setExtras(bundleOf(EXTRA_TRANSIENT to playbackState.transientLoss))

        // Do not add custom actions on Wear OS because there is a bug in Wear 3.5 that causes
        // this to make the Wear OS media notification stop working. This bug was fixed
        // internally by the Wear OS team in June 2023. Once that fix is released we should be
        // able to remove this guard.
        if (!Util.isWearOs(context)) {
            addCustomActions(stateBuilder, currentEpisode, playbackState)
        }

        return stateBuilder.build()
    }

    private fun getSupportedActions(playbackState: PlaybackState): Long {
        val prepareActions = PlaybackStateCompat.ACTION_PREPARE or
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH

        if (playbackState.isEmpty) {
            return PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                prepareActions
        } else if (shouldHideCustomSkipButtons()) {
            return PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                prepareActions
        } else {
            return PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
                prepareActions
        }
    }

    private fun updateUpNext(upNext: UpNextQueue.State) {
        try {
            mediaSession.setQueueTitle("Up Next")
            if (upNext is UpNextQueue.State.Loaded) {
                updateMetadata(upNext.episode)

                val items = upNext.queue.map { episode ->
                    val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else null
                    val podcast = podcastUuid?.let { podcastManager.findPodcastByUuid(it) }
                    val podcastTitle = episode.displaySubtitle(podcast)
                    val localUri = AutoConverter.getBitmapUriForPodcast(podcast, episode, context)
                    val description = MediaDescriptionCompat.Builder()
                        .setDescription(episode.episodeDescription)
                        .setTitle(episode.title)
                        .setSubtitle(podcastTitle)
                        .setMediaId(episode.uuid)
                        .setIconUri(localUri)
                        .build()

                    return@map MediaSessionCompat.QueueItem(description, episode.adapterId)
                }
                mediaSession.setQueue(items)
            } else {
                updateMetadata(null)
                mediaSession.setQueue(emptyList())

                val playbackStateCompat = getPlaybackStateCompat(PlaybackState(state = PlaybackState.State.EMPTY), currentEpisode = null)
                updatePlaybackState(playbackStateCompat)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun observePlaybackState() {
        val ignoreStates = listOf(
            // ignore buffer position because it isn't displayed in the media session
            "updateBufferPosition",
            // ignore the playback progress updates as the media session can calculate this without being sent it every second
            "updateCurrentPosition",
            // ignore the user seeking as the event onBufferingStateChanged will update the buffering state
            "onUserSeeking"
        )

        var previousEpisode: BaseEpisode? = null

        playbackManager.playbackStateRelay
            .observeOn(Schedulers.io())
            .switchMap { state ->
                val episodeSource =
                    if (state.isEmpty) {
                        Observable.just(Optional.empty())
                    } else {
                        episodeManager.observeEpisodeByUuidRx(state.episodeUuid)
                            .distinctUntilChanged(BaseEpisode.isMediaSessionEqual)
                            .map { Optional.of(it) }
                            // if the episode is deleted from the database while playing catch the error and just return an empty state
                            .onErrorReturn { Optional.empty() }
                            .toObservable()
                    }
                Observables.combineLatest(Observable.just(state), episodeSource)
            }
            .filter {
                !ignoreStates.contains(it.first.lastChangeFrom) || !BaseEpisode.isMediaSessionEqual(it.second.get(), previousEpisode)
            }
            .doOnNext {
                previousEpisode = it.second.get()
            }
            .switchMap { (state, episode) -> getPlaybackStateRx(state, episode).toObservable().onErrorResumeNext(Observable.empty()) }
            .switchMap {
                Observable.fromCallable { updatePlaybackState(it) }
                    .doOnError { LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Error updating playback state in media session: ${it.message}") }.retry(3)
            }
            .ignoreElements()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { throwable ->
                    LogBuffer.e(LogBuffer.TAG_PLAYBACK, "MEDIA SESSION ERROR: Error updating playback state: ${throwable.message}")
                }
            ).addTo(disposables)
    }

    private fun updateMetadata(episode: BaseEpisode?) {
        if (episode == null) {
            Timber.i("MediaSession metadata. Nothing Playing.")
            mediaSession.setMetadata(NOTHING_PLAYING)
            return
        }

        val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else null
        val podcast = podcastUuid?.let { podcastManager.findPodcastByUuid(it) }

        val podcastTitle = episode.displaySubtitle(podcast)
        val safeCharacterPodcastTitle = podcastTitle.replace("%", "pct")
        var nowPlayingBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, episode.uuid)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, safeCharacterPodcastTitle)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, episode.durationMs.toLong())
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Podcast")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, episode.title)

        if (podcast != null && podcast.author.isNotEmpty()) {
            nowPlayingBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, podcast.author)
        }

        val nowPlaying = nowPlayingBuilder.build()
        Timber.i("MediaSession metadata. Episode: ${episode.uuid} ${episode.title} Duration: ${episode.durationMs.toLong()}")
        mediaSession.setMetadata(nowPlaying)

        if (settings.showArtworkOnLockScreen.value) {
            if (Util.isAutomotive(context)) {
                val bitmapUri = AutoConverter.getBitmapUriForPodcast(podcast, episode, context)?.toString()
                nowPlayingBuilder = nowPlayingBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, bitmapUri)
                nowPlayingBuilder = nowPlayingBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, bitmapUri)
            } else {
                val bitmap = AutoConverter.getBitmapForPodcast(podcast, false, context)
                nowPlayingBuilder = nowPlayingBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }

            val nowPlayingWithArtwork = nowPlayingBuilder.build()
            Timber.i("MediaSession metadata. With artwork.")
            mediaSession.setMetadata(nowPlayingWithArtwork)
        }
    }

    private fun addCustomActions(stateBuilder: PlaybackStateCompat.Builder, currentEpisode: BaseEpisode, playbackState: PlaybackState) {
        if (!shouldHideCustomSkipButtons()) {
            addCustomAction(stateBuilder, APP_ACTION_SKIP_BACK, "Skip back", IR.drawable.auto_skipback)
            addCustomAction(stateBuilder, APP_ACTION_SKIP_FWD, "Skip forward", IR.drawable.auto_skipforward)
        }

        val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
        settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
            when (mediaControl) {
                MediaNotificationControls.Archive -> addCustomAction(stateBuilder, APP_ACTION_ARCHIVE, "Archive", IR.drawable.ic_archive)
                MediaNotificationControls.MarkAsPlayed -> addCustomAction(stateBuilder, APP_ACTION_MARK_AS_PLAYED, "Mark as played", IR.drawable.auto_markasplayed)
                MediaNotificationControls.PlayNext -> addCustomAction(stateBuilder, APP_ACTION_PLAY_NEXT, "Play next", com.google.android.gms.cast.framework.R.drawable.cast_ic_mini_controller_skip_next)
                MediaNotificationControls.PlaybackSpeed -> {
                    if (playbackManager.isAudioEffectsAvailable()) {
                        val currentSpeed = playbackState.playbackSpeed
                        val drawableId = when {
                            currentSpeed <= 1 -> IR.drawable.auto_1x
                            currentSpeed == 1.1 -> IR.drawable.auto_1_1x
                            currentSpeed == 1.2 -> IR.drawable.auto_1_2x
                            currentSpeed == 1.3 -> IR.drawable.auto_1_3x
                            currentSpeed == 1.4 -> IR.drawable.auto_1_4x
                            currentSpeed == 1.5 -> IR.drawable.auto_1_5x
                            currentSpeed == 1.6 -> IR.drawable.auto_1_6x
                            currentSpeed == 1.7 -> IR.drawable.auto_1_7x
                            currentSpeed == 1.8 -> IR.drawable.auto_1_8x
                            currentSpeed == 1.9 -> IR.drawable.auto_1_9x
                            currentSpeed == 2.0 -> IR.drawable.auto_2x
                            currentSpeed == 2.1 -> IR.drawable.auto_2_1x
                            currentSpeed == 2.2 -> IR.drawable.auto_2_2x
                            currentSpeed == 2.3 -> IR.drawable.auto_2_3x
                            currentSpeed == 2.4 -> IR.drawable.auto_2_4x
                            currentSpeed == 2.5 -> IR.drawable.auto_2_5x
                            currentSpeed == 2.6 -> IR.drawable.auto_2_6x
                            currentSpeed == 2.7 -> IR.drawable.auto_2_7x
                            currentSpeed == 2.8 -> IR.drawable.auto_2_8x
                            currentSpeed == 2.9 -> IR.drawable.auto_2_9x
                            currentSpeed == 3.0 -> IR.drawable.auto_3x
                            else -> IR.drawable.auto_1x
                        }

                        stateBuilder.addCustomAction(APP_ACTION_CHANGE_SPEED, "Change speed", drawableId)
                    }
                }
                MediaNotificationControls.Star -> {
                    if (currentEpisode is PodcastEpisode) {
                        if (currentEpisode.isStarred) {
                            addCustomAction(stateBuilder, APP_ACTION_UNSTAR, "Unstar", IR.drawable.auto_starred)
                        } else {
                            addCustomAction(stateBuilder, APP_ACTION_STAR, "Star", IR.drawable.auto_star)
                        }
                    }
                }
            }
        }
    }

    private fun addCustomAction(stateBuilder: PlaybackStateCompat.Builder, action: String, name: CharSequence, @DrawableRes icon: Int) {
        val addToWearExtras = Bundle().apply {
            putBoolean("android.support.wearable.media.extra.CUSTOM_ACTION_SHOW_ON_WEAR", true)
        }

        val skipBackBuilder = PlaybackStateCompat.CustomAction.Builder(action, name, icon).apply {
            setExtras(addToWearExtras)
        }
        stateBuilder.addCustomAction(skipBackBuilder.build())
    }

    inner class MediaSessionCallback(
        val playbackManager: PlaybackManager,
        val episodeManager: EpisodeManager
    ) : MediaSessionCompat.Callback() {

        private var playPauseTimer: Timer? = null
        private var playFromSearchDisposable: Disposable? = null

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            if (Intent.ACTION_MEDIA_BUTTON == mediaButtonEvent.action) {
                val keyEvent = IntentCompat.getParcelableExtra(mediaButtonEvent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    ?: return false
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            handleMediaButtonSingleTap()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            handleMediaButtonDoubleTap()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            handleMediaButtonTripleTap()
                            return true
                        }
                    }
                }
            }

            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        private fun onAddBookmark() {
            logEvent("add bookmark")
            val coroutineContext = CoroutineScope(Dispatchers.Main + Job())
            coroutineContext.launch {
                Util.isAndroidAutoConnectedFlow(context).collect {
                    bookmarkHelper.handleAddBookmarkAction(context, it)
                    // Cancel the coroutine after the bookmark has been added
                    coroutineContext.cancel()
                }
            }
        }

        private fun getCurrentControllerInfo(): String {
            val info = mediaSession.currentControllerInfo
            return "Controller: ${info.packageName} pid: ${info.pid} uid: ${info.uid}"
        }

        // The parameter inSessionCallback can only be set to true if being called from the MediaSession.Callback thread. The method getCurrentControllerInfo() can only be called from this thread.
        private fun logEvent(action: String, inSessionCallback: Boolean = true) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Event from Media Session to $action. ${if (inSessionCallback) getCurrentControllerInfo() else ""}")
        }

        private fun handleMediaButtonSingleTap() {
            // this code allows the user to double tap their play pause button to skip ahead. Basically it allows them 600ms to press it again to cause a skip instead of a play/pause
            if (playPauseTimer == null) {
                playPauseTimer = Timer().apply {
                    schedule(
                        object : TimerTask() {
                            override fun run() {
                                logEvent("play from headset hook", inSessionCallback = false)
                                playbackManager.playPause(sourceView = source)
                                playPauseTimer = null
                            }
                        },
                        600
                    )
                }
            } else {
                // timer is not null, which means they pressed play pause in the last 300ms, fire a next instead
                playPauseTimer?.cancel()
                playPauseTimer = null

                playbackManager.skipForward(sourceView = source)
            }
        }

        private fun handleMediaButtonDoubleTap() {
            handleMediaButtonAction(settings.headphoneControlsNextAction.value)
        }

        private fun handleMediaButtonTripleTap() {
            handleMediaButtonAction(settings.headphoneControlsPreviousAction.value)
        }

        private fun handleMediaButtonAction(action: HeadphoneAction) {
            when (action) {
                HeadphoneAction.ADD_BOOKMARK -> onAddBookmark()
                HeadphoneAction.SKIP_FORWARD -> onSkipToNext()
                HeadphoneAction.SKIP_BACK -> onSkipToPrevious()
                HeadphoneAction.NEXT_CHAPTER,
                HeadphoneAction.PREVIOUS_CHAPTER -> Timber.e(ACTION_NOT_SUPPORTED)
            }
        }

        // We don't need to do anything special to prepare but this will make things
        // faster apparently. The Google Sample App UAMP does the same
        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            super.onPrepareFromSearch(query, extras)
            onPlayFromSearch(query, extras)
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPrepareFromMediaId(mediaId, extras)
            onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlay() {
            logEvent("play")
            playbackManager.playQueue(sourceView = source)
        }

        override fun onPause() {
            logEvent("pause")
            playbackManager.pause(sourceView = source)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            logEvent("play from search")
            playFromSearchDisposable?.dispose()
            playFromSearchDisposable = performPlayFromSearchRx(query)
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { Timber.e(it) })
        }

        override fun onStop() {
            logEvent("stop")
            launch {
                // note: the stop event is called from cars when they only want to pause, this is less destructive and doesn't cause issues if they try to play again
                playbackManager.pause(sourceView = source)
            }
        }

        override fun onSkipToPrevious() {
            logEvent("skip backwards")
            playbackManager.skipBackward(sourceView = source)
        }

        override fun onSkipToNext() {
            logEvent("skip forwards")
            playbackManager.skipForward(sourceView = source)
        }

        override fun onSetRating(rating: RatingCompat?) {
            super.onSetRating(rating)

            if (rating?.hasHeart() == true) {
                starEpisode()
            } else {
                unstarEpisode()
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            mediaId ?: return
            launch {
                logEvent("play from media id", inSessionCallback = false)

                val autoMediaId = AutoMediaId.fromMediaId(mediaId)
                val episodeId = autoMediaId.episodeId
                episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                    playbackManager.playNow(episode, sourceView = source)
                    LastPlayedList.fromString(autoMediaId.sourceId).let { lastPlayedList ->
                        settings.lastLoadedFromPodcastOrFilterUuid.set(lastPlayedList)
                    }
                }
            }
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            action ?: return

            when (action) {
                APP_ACTION_SKIP_BACK -> playbackManager.skipBackward()
                APP_ACTION_SKIP_FWD -> playbackManager.skipForward()
                APP_ACTION_MARK_AS_PLAYED -> markAsPlayed()
                APP_ACTION_STAR -> starEpisode()
                APP_ACTION_UNSTAR -> unstarEpisode()
                APP_ACTION_CHANGE_SPEED -> changePlaybackSpeed()
                APP_ACTION_ARCHIVE -> archive()
                APP_ACTION_PLAY_NEXT -> playbackManager.playNextInQueue()
            }
        }

        override fun onSkipToQueueItem(id: Long) {
            val state = playbackManager.upNextQueue.changesObservable.blockingFirst()
            if (state is UpNextQueue.State.Loaded) {
                state.queue.find { it.adapterId == id }?.let { episode ->
                    logEvent("play from skip to queue item")
                    playbackManager.playNow(episode = episode, sourceView = source)
                }
            }
        }

        override fun onSeekTo(pos: Long) {
            logEvent("seek to $pos")
            launch {
                playbackManager.seekToTimeMs(pos.toInt())
                playbackManager.trackPlaybackSeek(pos.toInt(), SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
            }
        }
    }

    private fun markAsPlayed() {
        launch {
            val episode = playbackManager.getCurrentEpisode()
            episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
            episode?.let {
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_PLAYED, source, it.uuid)
            }
        }
    }

    private fun starEpisode() {
        launch {
            playbackManager.getCurrentEpisode()?.let {
                if (it is PodcastEpisode) {
                    it.isStarred = true
                    episodeManager.starEpisode(episode = it, starred = true, sourceView = source)
                }
            }
        }
    }

    private fun unstarEpisode() {
        launch {
            playbackManager.getCurrentEpisode()?.let {
                if (it is PodcastEpisode) {
                    it.isStarred = false
                    episodeManager.starEpisode(episode = it, starred = false, sourceView = source)
                }
            }
        }
    }

    private fun changePlaybackSpeed() {
        launch {
            val speed = playbackManager.getPlaybackSpeed()
            val newSpeed = when {
                speed < 1.2 -> 1.2
                speed < 1.4 -> 1.4
                speed < 1.6 -> 1.6
                speed < 1.8 -> 1.8
                speed < 2 -> 2.0
                else -> 1.0
            }

            val episode = playbackManager.getCurrentEpisode() ?: return@launch
            if (episode is PodcastEpisode) {
                // update per podcast playback speed
                val podcast = podcastManager.findPodcastByUuidSuspend(episode.podcastUuid)
                if (podcast != null && podcast.overrideGlobalEffects) {
                    podcast.playbackSpeed = newSpeed
                    podcastManager.updatePlaybackSpeed(podcast = podcast, speed = newSpeed)
                    playbackManager.updatePlayerEffects(effects = podcast.playbackEffects)
                    return@launch
                }
            }
            // update global playback speed
            val effects = settings.globalPlaybackEffects.value
            effects.playbackSpeed = newSpeed
            settings.globalPlaybackEffects.set(effects)
            playbackManager.updatePlayerEffects(effects = effects)
        }
    }

    private fun archive() {
        launch {
            playbackManager.getCurrentEpisode()?.let {
                if (it is PodcastEpisode) {
                    it.isArchived = true
                    episodeManager.archive(it, playbackManager)
                    episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ARCHIVED, source, it.uuid)
                }
            }
        }
    }

    fun playFromSearchExternal(extras: Bundle) {
        val searchTerm = extras.getString(SearchManager.QUERY) ?: return
        performPlayFromSearch(searchTerm)
    }

    /**
     * Test the search using the following terminal command
     * adb shell am start -a android.media.action.MEDIA_PLAY_FROM_SEARCH -p au.com.shiftyjelly.pocketcasts -n au.com.shiftyjelly.pocketcasts/.ui.MainActivity --es query "next\ episode\ in"
     * In the debug you can use the following
     * adb shell am start -a android.media.action.MEDIA_PLAY_FROM_SEARCH -p au.com.shiftyjelly.pocketcasts.debug --es query "The\ Daily\ in"
     * Say the phrase ‘OK, Google’ followed by one of the following
     * ‘Listen to [podcast name] in Pocket Casts’
     * ‘Listen to [filter name] in Pocket Casts’
     * ‘Listen to Up Next in Pocket Casts’
     * ‘Play Up Next in Pocket Casts’
     * ‘Play New Releases Next in Pocket Casts’
     */
    private fun performPlayFromSearch(searchTerm: String?) {
        Timber.d("performSearch $searchTerm")
        val query: String = searchTerm?.trim { it <= ' ' }?.lowercase() ?: return

        Timber.i("performPlayFromSearch query: $query")

        val sourceView = SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION
        launch {
            if (query.startsWith("up next")) {
                playbackManager.playQueue(sourceView = sourceView)
                return@launch
            }

            if (query.startsWith("next episode") || query.startsWith("next podcast")) {
                val queueEpisodes = playbackManager.upNextQueue.queueEpisodes
                queueEpisodes.firstOrNull()?.let { episode ->
                    launch { playbackManager.playNext(episode = episode, source = source) }
                    return@launch
                }
            }

            val options = calculateSearchQueryOptions(query)
            for (option in options) {
                val matchingPodcast: Podcast? = podcastManager.searchPodcastByTitle(option)
                if (matchingPodcast != null) {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played podcast from search %s.", option)
                    playPodcast(podcast = matchingPodcast, sourceView = sourceView)
                    return@launch
                }
            }

            for (option in options) {
                val matchingEpisode = episodeManager.findFirstBySearchQuery(option) ?: continue
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played episode from search %s.", option)
                playbackManager.playNow(episode = matchingEpisode, sourceView = sourceView)
                return@launch
            }

            for (option in options) {
                val playlist = playlistManager.findFirstByTitle(option) ?: continue

                Timber.i("Playing matched playlist '$option'")

                val episodeCount = playlistManager.countEpisodes(playlist.id, episodeManager, playbackManager)
                if (episodeCount == 0) return@launch

                val episodesToPlay = playlistManager.findEpisodes(playlist, episodeManager, playbackManager).take(5)
                if (episodesToPlay.isEmpty()) return@launch

                playEpisodes(episodesToPlay, sourceView)

                return@launch
            }

            withContext(Dispatchers.Main) {
                Timber.i("No search results")
                // couldn't find a match if we get here
                errorPlaybackState("No search results")
            }
        }
    }

    private fun errorPlaybackState(message: String) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
            .setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, message)
            .setActions(getSupportedActions(PlaybackState()))

        Timber.i("MediaSession playback state. Error: $message")
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun performPlayFromSearchRx(searchTerm: String?): Completable {
        return Completable.fromAction { performPlayFromSearch(searchTerm) }
    }

    private fun playEpisodes(episodes: List<PodcastEpisode>, sourceView: SourceView) {
        if (episodes.isEmpty()) {
            return
        }

        playbackManager.playEpisodes(episodes = episodes, sourceView = sourceView)
    }

    private suspend fun playPodcast(podcast: Podcast, sourceView: SourceView = SourceView.UNKNOWN) {
        val latestEpisode = withContext(Dispatchers.Default) { episodeManager.findLatestUnfinishedEpisodeByPodcast(podcast) } ?: return
        playbackManager.playNow(episode = latestEpisode, sourceView = sourceView)
    }

    private fun shouldHideCustomSkipButtons(): Boolean {
        return MANUFACTURERS_TO_HIDE_CUSTOM_SKIP_BUTTONS.contains(Build.MANUFACTURER.lowercase())
    }
}

private const val APP_ACTION_STAR = "star"
private const val APP_ACTION_UNSTAR = "unstar"
private const val APP_ACTION_SKIP_BACK = "jumpBack"
private const val APP_ACTION_SKIP_FWD = "jumpFwd"
private const val APP_ACTION_MARK_AS_PLAYED = "markAsPlayed"
private const val APP_ACTION_CHANGE_SPEED = "changeSpeed"
private const val APP_ACTION_ARCHIVE = "archive"
private const val APP_ACTION_PLAY_NEXT = "playNext"

private val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()
