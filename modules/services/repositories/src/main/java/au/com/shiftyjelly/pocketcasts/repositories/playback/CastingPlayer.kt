package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import androidx.media3.common.Player
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class CastingPlayer(
    val context: Context,
    override val onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
    player: Player,
) : PocketCastsPlayer, Player by player {

    private var customData: JSONObject? = null
    private var podcast: Podcast? = null
    private var episode: Playable? = null
    private var state: Int = 0
    private var remoteListenerAdded: Boolean = false
    private var localEpisodeUuid: String? = null
    private var remoteEpisodeUuid: String? = null
    private var playbackEffects: PlaybackEffects? = null
    private val remoteMediaClientListener = RemoteMediaClientListener()

    override var isPip: Boolean = false

    private val sessionManager: SessionManager?
        get() = CastContext.getSharedInstance()?.sessionManager

    private val castSession: CastSession?
        get() = sessionManager?.currentCastSession

    private val remoteMediaClient: RemoteMediaClient?
        get() = castSession?.remoteMediaClient

    private val isMediaLoaded: Boolean
        get() = isConnected &&
            (remoteMediaClient?.hasMediaSession() ?: false) &&
            TextUtils.equals(localEpisodeUuid, remoteEpisodeUuid) &&
            state != PlaybackStateCompat.STATE_NONE && state != PlaybackStateCompat.STATE_STOPPED

    override val isRemote: Boolean
        get() = true

    override val isStreaming: Boolean
        get() = true

    override val name: String
        get() = "Cast"

    override var playable: Playable? = null
        set(value) {
            field = value
            localEpisodeUuid = value?.uuid
            buildCustomData()
        }

    private val isConnected: Boolean
        get() {
            val castSession = CastContext.getSharedInstance()?.sessionManager?.currentCastSession
            return castSession != null && castSession.isConnected
        }

    init {
        if (remoteMediaClient != null) {
            addRemoteMediaListener()
            setMetadataFromRemote()
        }
    }

    fun updateFromRemoteIfRequired() {
        if (remoteEpisodeUuid != null) {
            updatePlaybackState()
        }
    }

    override fun isPlaying(): Boolean =
        isMediaLoaded && (remoteMediaClient?.isPlaying ?: false)

    override suspend fun bufferedUpToMs(): Int = 0

    override suspend fun bufferedPercentage(): Int = 0

    override suspend fun durationMs(): Int? = episode?.durationMs

    override suspend fun isBuffering(): Boolean =
        state == PlaybackStateCompat.STATE_BUFFERING

    override suspend fun getCurrentPositionMs(): Int {
        return withContext(Dispatchers.Main) {
            if (!isMediaLoaded) {
                return@withContext -1
            }

            val streamPositionMs = remoteMediaClient?.approximateStreamPosition?.toInt() ?: 0
            if (streamPositionMs <= 0) {
                return@withContext -1
            }

            updateMissingDuration()
            return@withContext streamPositionMs
        }
    }

    override suspend fun load(currentPositionMs: Int) {}

    override suspend fun play(currentPositionMs: Int) {
        withContext(Dispatchers.Main) {
            episode?.let {
                state = PlaybackStateCompat.STATE_BUFFERING
                loadEpisode(it.uuid, currentPositionMs, true)
            }
        }
    }

    override fun pause() {
        if (isMediaLoaded) {
            remoteMediaClient?.pause()
        }
    }

    override fun stop() {
        state = PlaybackStateCompat.STATE_STOPPED
        remoteListenerAdded = false
        remoteMediaClient?.unregisterCallback(remoteMediaClientListener)
    }

    override fun supportsTrimSilence(): Boolean = false

    override fun supportsVolumeBoost(): Boolean = false

    override fun supportsVideo(): Boolean = true

    override suspend fun seekToTimeMs(positionMs: Int) {
        withContext(Dispatchers.Main) {
            if (isMediaLoaded) {
                val seekOptions = MediaSeekOptions.Builder()
                    .setPosition(positionMs.toLong())
                    .setCustomData(customData)
                    .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
                    .build()
                remoteMediaClient?.seek(seekOptions)
            } else {
                localEpisodeUuid?.let {
                    loadEpisode(it, positionMs, true)
                }
            }
        }
    }

    override suspend fun setPlaybackEffects(playbackEffects: PlaybackEffects) {
        withContext(Dispatchers.Main) {
            this@CastingPlayer.playbackEffects = playbackEffects
            setPlayerEffects()
        }
    }

    private fun setPlayerEffects() {
        if (playbackEffects == null) {
            return
        }
        remoteMediaClient?.setPlaybackRate(calcPlaybackSpeed())
    }

    private fun calcPlaybackSpeed(): Double {
        val playbackEffects: PlaybackEffects =
            playbackEffects ?: return 1.0
        // check we are in the limits
        var speed = playbackEffects.playbackSpeed
        if (speed > MediaLoadOptions.PLAYBACK_RATE_MAX) {
            speed = MediaLoadOptions.PLAYBACK_RATE_MAX
        } else if (speed < MediaLoadOptions.PLAYBACK_RATE_MIN) {
            speed = MediaLoadOptions.PLAYBACK_RATE_MIN
        }
        return speed
    }

    override fun setVolume(volume: Float) {
    }

    override fun setPodcast(podcast: Podcast?) {
        this.podcast = podcast
    }

    private fun addRemoteMediaListener() {
        if (remoteListenerAdded) {
            return
        }
        remoteListenerAdded = true
        remoteMediaClient?.registerCallback(remoteMediaClientListener)
    }

    private fun loadEpisode(episodeUuid: String, currentPositionMs: Int, autoPlay: Boolean) {
        if (episodeUuid == remoteEpisodeUuid && autoPlay) {
            remoteMediaClient?.play()
            return
        }
        localEpisodeUuid = episodeUuid
        val episode = episode
        val podcast = podcast
        if (episode == null || podcast == null || episodeUuid != episode.uuid) {
            return
        }
        val url = episode.downloadUrl ?: return
        if (episode is UserEpisode && (episode.serverStatus != UserEpisodeServerStatus.UPLOADED || episode.downloadUrl == null)) {
            onPlayerEvent(this, PlayerEvent.PlayerError("Unable to cast local file"))
            return
        }
        val mediaInfo = buildMediaInfo(url, episode, podcast)
        val loadOptions = MediaLoadOptions.Builder()
            .setAutoplay(autoPlay)
            .setPlaybackRate(calcPlaybackSpeed())
            .setPlayPosition(currentPositionMs.toLong())
            .setCustomData(customData)
            .build()
        remoteMediaClient?.load(mediaInfo, loadOptions)
            ?.setResultCallback { addRemoteMediaListener() }
    }

    private fun buildMediaInfo(url: String, episode: Playable, podcast: Podcast): MediaInfo {
        val mediaMetadata = MediaMetadata(if (episode.isVideo) MediaMetadata.MEDIA_TYPE_MOVIE else MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, episode.title)
            putString(MediaMetadata.KEY_SUBTITLE, podcast.title)
            putString(MediaMetadata.KEY_ALBUM_ARTIST, podcast.author)
            putString(MediaMetadata.KEY_ALBUM_TITLE, podcast.title)
            addImage(WebImage(Uri.parse(podcast.getArtworkUrl(960))))
        }
        var mediaInfo = MediaInfo.Builder(url).setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(mediaMetadata)
        episode.fileType?.let {
            mediaInfo = mediaInfo.setContentType(it)
        }
        customData?.let {
            mediaInfo = mediaInfo.setCustomData(it)
        }
        return mediaInfo.build()
    }

    private fun buildCustomData() {
        customData = JSONObject().apply {
            try {
                put(CUSTOM_DATA_EPISODE_UUID, episode?.uuid)
            } catch (e: JSONException) {
                Timber.e(e)
            }
        }
    }

    private fun setMetadataFromRemote() {
        Timber.d("Cast remote metadata available $remoteMediaClient")
        val remoteMediaClient = remoteMediaClient ?: return
        // Sync: We get the customData from the remote media information and update the local metadata if it happens to be different from the one we are currently using.
        // This can happen when the app was either restarted/disconnected + connected, or if the app joins an existing session while the Chromecast was playing a queue.
        remoteEpisodeUuid = getRemoteEpisodeUuid(remoteMediaClient)
    }

    private fun getRemoteEpisodeUuid(remoteMediaClient: RemoteMediaClient): String? {
        try {
            val mediaInfo = remoteMediaClient.mediaInfo ?: return null
            val customData = mediaInfo.customData
            if (customData != null && customData.has(CUSTOM_DATA_EPISODE_UUID)) {
                return customData.getString(CUSTOM_DATA_EPISODE_UUID)
            }
        } catch (e: JSONException) {
            Timber.e(e, "Exception processing update metadata")
        }

        return null
    }

    private fun updatePlaybackState() {
        val remoteMediaClient = remoteMediaClient
        if (remoteMediaClient == null) {
            onPlayerEvent(this, PlayerEvent.PlayerPaused)
            return
        }

        val status = remoteMediaClient.playerState
        val idleReason = remoteMediaClient.idleReason
        val remoteEpisodeUuid = getRemoteEpisodeUuid(remoteMediaClient)

        if (localEpisodeUuid == null && remoteEpisodeUuid != null) {
            Timber.d("Remote has episode while local player is null. Remote: $remoteEpisodeUuid")
            onPlayerEvent(this, PlayerEvent.RemoteMetadataNotMatched(remoteEpisodeUuid))
            return
        }

        // Convert the remote playback states to media playback states.
        when (status) {
            MediaStatus.PLAYER_STATE_IDLE -> if (idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                onPlayerEvent(this, PlayerEvent.Completion(playable?.uuid))
            }
            MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.PLAYER_STATE_LOADING -> {
                state = PlaybackStateCompat.STATE_BUFFERING
                onPlayerEvent(this, PlayerEvent.BufferingStateChanged)
            }
            MediaStatus.PLAYER_STATE_PLAYING -> {
                state = PlaybackStateCompat.STATE_PLAYING
                setMetadataFromRemote()
                onPlayerEvent(this, PlayerEvent.PlayerPlaying)
            }
            MediaStatus.PLAYER_STATE_PAUSED -> {
                state = PlaybackStateCompat.STATE_PAUSED
                setMetadataFromRemote()
                onPlayerEvent(this, PlayerEvent.PlayerPaused)
            }
            else -> {
                Timber.w("Unknown Cast event $status")
            }
        }

        // the MediaStatus of PLAYER_STATE_BUFFERING only reports when the media is buffering and not when it has stopped
        if (status != MediaStatus.PLAYER_STATE_BUFFERING) {
            onPlayerEvent(this, PlayerEvent.BufferingStateChanged)
        }
    }

    private fun updateMissingDuration() {
        if ((episode?.durationMs ?: 0) > 0) {
            return
        }
        val duration = if (!isMediaLoaded) 0 else (remoteMediaClient?.streamDuration?.toInt() ?: 0)
        if (duration > 0) {
            onPlayerEvent(this, PlayerEvent.DurationAvailable)
            episode?.durationMs = duration
        }
    }

    private inner class RemoteMediaClientListener : RemoteMediaClient.Callback() {
        override fun onMetadataUpdated() {
            Timber.d("Remote meta data updated")
            setMetadataFromRemote()
            if (localEpisodeUuid == null) {
                updatePlaybackState()
            }
        }

        override fun onStatusUpdated() {
            updatePlaybackState()
        }

        override fun onSendingRemoteMediaRequest() {}

        override fun onAdBreakStatusUpdated() {}

        override fun onQueueStatusUpdated() {}

        override fun onPreloadStatusUpdated() {}
    }

    companion object {
        private const val CUSTOM_DATA_EPISODE_UUID = "EPISODE_UUID"
    }
}
