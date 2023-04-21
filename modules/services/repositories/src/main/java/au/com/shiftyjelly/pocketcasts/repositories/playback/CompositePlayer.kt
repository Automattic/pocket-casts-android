package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber
import kotlin.math.min

/**
 * Based on https://github.com/android/uamp/blob/media3/common/src/main/java/com/example/android/uamp/media/ReplaceableForwardingPlayer.kt
 */
@androidx.annotation.OptIn(UnstableApi::class)
class CompositePlayer(
    private val castPlayer: CastPlayer?,
    private val localPlayer: ExoPlayer,
) : Player {

    private var currentPlayer: Player = if (castPlayer?.isCastSessionAvailable == true) {
        castPlayer
    } else {
        localPlayer
    }

    enum class PlayerType { LOCAL, CAST }
    private val listeners: MutableList<Player.Listener> = mutableListOf()
    // After disconnecting from the Cast device, the timeline of the CastPlayer is empty, so we
    // need to track the playlist to be able to transfer the playlist back to the local player after
    // having disconnected.
    private val playlist: MutableList<MediaItem> = arrayListOf()

    fun setCurrentPlayer(playerType: PlayerType) {
        val newPlayer = when (playerType) {
            PlayerType.LOCAL -> localPlayer
            PlayerType.CAST -> {
                castPlayer ?: run {
                    Timber.i("Cannot switch to CastPlayer, no CastPlayer available")
                    return
                }
            }
        }

        if (newPlayer == currentPlayer) {
            Timber.i("Player already set to $playerType")
            return
        }

        listeners.forEach { listener ->
            currentPlayer.removeListener(listener)
            newPlayer.addListener(listener)
        }

        newPlayer.repeatMode = currentPlayer.repeatMode
        newPlayer.shuffleModeEnabled = currentPlayer.shuffleModeEnabled
        newPlayer.playlistMetadata = currentPlayer.playlistMetadata
        newPlayer.trackSelectionParameters = currentPlayer.trackSelectionParameters
        newPlayer.volume = currentPlayer.volume
        newPlayer.playWhenReady = currentPlayer.playWhenReady

        // Prepare the new player.
        newPlayer.setMediaItems(playlist, currentMediaItemIndex, currentPlayer.contentPosition)
        newPlayer.prepare()

        // Stop the previous player. Don't release so it can be used again.
        currentPlayer.clearMediaItems()
        currentPlayer.stop()

        currentPlayer = newPlayer
    }

    override fun getApplicationLooper(): Looper =
        currentPlayer.applicationLooper

    override fun addListener(listener: Player.Listener) {
        currentPlayer.addListener(listener)
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        currentPlayer.removeListener(listener)
        listeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        currentPlayer.setMediaItems(mediaItems)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        currentPlayer.setMediaItems(mediaItems, resetPosition)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        currentPlayer.setMediaItems(mediaItems, startIndex, startPositionMs)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        currentPlayer.setMediaItem(mediaItem)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        currentPlayer.setMediaItem(mediaItem, startPositionMs)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        currentPlayer.setMediaItem(mediaItem, resetPosition)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        currentPlayer.addMediaItem(mediaItem)
        playlist.add(mediaItem)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        currentPlayer.addMediaItem(index, mediaItem)
        playlist.add(index, mediaItem)
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        currentPlayer.addMediaItems(mediaItems)
        playlist.addAll(mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        currentPlayer.addMediaItems(index, mediaItems)
        playlist.addAll(index, mediaItems)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        currentPlayer.moveMediaItem(currentIndex, newIndex)
        playlist.add(newIndex, playlist.removeAt(currentIndex))
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        val removedItems: ArrayDeque<MediaItem> = ArrayDeque()
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            removedItems.addFirst(playlist.removeAt(fromIndex + i))
        }
        playlist.addAll(min(newIndex, playlist.size), removedItems)
    }

    override fun removeMediaItem(index: Int) {
        currentPlayer.removeMediaItem(index)
        playlist.removeAt(index)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        currentPlayer.removeMediaItems(fromIndex, toIndex)
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            playlist.removeAt(fromIndex + i)
        }
    }

    override fun clearMediaItems() {
        currentPlayer.clearMediaItems()
        playlist.clear()
    }

    override fun isCommandAvailable(command: Int): Boolean =
        currentPlayer.isCommandAvailable(command)

    override fun canAdvertiseSession(): Boolean =
        currentPlayer.canAdvertiseSession()

    override fun getAvailableCommands(): Player.Commands =
        currentPlayer.availableCommands

    override fun prepare() {
        currentPlayer.prepare()
    }

    override fun getPlaybackState(): Int =
        currentPlayer.playbackState

    override fun getPlaybackSuppressionReason(): Int =
        currentPlayer.playbackSuppressionReason

    override fun isPlaying(): Boolean =
        currentPlayer.isPlaying

    override fun getPlayerError(): PlaybackException? =
        currentPlayer.playerError

    override fun play() {
        currentPlayer.play()
    }

    override fun pause() {
        currentPlayer.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        currentPlayer.playWhenReady = playWhenReady
    }

    override fun getPlayWhenReady(): Boolean =
        currentPlayer.playWhenReady

    override fun setRepeatMode(repeatMode: Int) {
        currentPlayer.repeatMode = repeatMode
    }

    override fun getRepeatMode(): Int =
        currentPlayer.repeatMode

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        currentPlayer.shuffleModeEnabled = shuffleModeEnabled
    }

    override fun getShuffleModeEnabled(): Boolean =
        currentPlayer.shuffleModeEnabled

    override fun isLoading(): Boolean =
        currentPlayer.isLoading

    override fun seekToDefaultPosition() {
        currentPlayer.seekToDefaultPosition()
    }

    override fun seekToDefaultPosition(windowIndex: Int) {
        currentPlayer.seekToDefaultPosition(windowIndex)
    }

    override fun seekTo(positionMs: Long) {
        currentPlayer.seekTo(positionMs)
    }

    override fun seekTo(windowIndex: Int, positionMs: Long) {
        currentPlayer.seekTo(windowIndex, positionMs)
    }

    override fun getSeekBackIncrement(): Long =
        currentPlayer.seekBackIncrement

    override fun seekBack() {
        currentPlayer.seekBack()
    }

    override fun getSeekForwardIncrement(): Long =
        currentPlayer.seekForwardIncrement

    override fun seekForward() {
        currentPlayer.seekForward()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun hasPrevious(): Boolean =
        currentPlayer.hasPrevious()

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun hasPreviousWindow(): Boolean =
        currentPlayer.hasPreviousWindow()

    override fun hasPreviousMediaItem(): Boolean =
        currentPlayer.hasPreviousMediaItem()

    @Deprecated("Deprecated in Java")
    override fun previous() {
        @Suppress("DEPRECATION")
        currentPlayer.previous()
    }

    @Deprecated("Deprecated in Java")
    override fun seekToPreviousWindow() {
        @Suppress("DEPRECATION")
        currentPlayer.seekToPreviousWindow()
    }

    override fun seekToPreviousMediaItem() {
        currentPlayer.seekToPreviousMediaItem()
    }

    override fun getMaxSeekToPreviousPosition(): Long =
        currentPlayer.maxSeekToPreviousPosition

    override fun seekToPrevious() {
        currentPlayer.seekToPrevious()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun hasNext(): Boolean =
        currentPlayer.hasNext()

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun hasNextWindow(): Boolean =
        currentPlayer.hasNextWindow()

    override fun hasNextMediaItem(): Boolean =
        currentPlayer.hasNextMediaItem()

    @Deprecated("Deprecated in Java")
    override fun next() {
        @Suppress("DEPRECATION")
        currentPlayer.next()
    }

    @Deprecated("Deprecated in Java")
    override fun seekToNextWindow() {
        @Suppress("DEPRECATION")
        currentPlayer.seekToNextWindow()
    }

    override fun seekToNextMediaItem() {
        currentPlayer.seekToNextMediaItem()
    }

    override fun seekToNext() {
        currentPlayer.seekToNext()
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        currentPlayer.playbackParameters = playbackParameters
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentPlayer.setPlaybackSpeed(speed)
    }

    override fun getPlaybackParameters(): PlaybackParameters =
        currentPlayer.playbackParameters

    override fun stop() {
        currentPlayer.stop()
    }

    @Deprecated("Deprecated in Java")
    override fun stop(reset: Boolean) {
        @Suppress("DEPRECATION")
        currentPlayer.stop(reset)
        if (reset) {
            playlist.clear()
        }
    }

    override fun release() {
        currentPlayer.release()
        playlist.clear()
    }

    override fun getCurrentTracks(): Tracks =
        currentPlayer.currentTracks

    override fun getTrackSelectionParameters(): TrackSelectionParameters =
        currentPlayer.trackSelectionParameters

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        currentPlayer.trackSelectionParameters = parameters
    }

    override fun getMediaMetadata(): MediaMetadata =
        currentPlayer.mediaMetadata

    override fun getPlaylistMetadata(): MediaMetadata =
        currentPlayer.playlistMetadata

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        currentPlayer.playlistMetadata = mediaMetadata
    }

    @Deprecated("Deprecated in Java")
    override fun getCurrentManifest(): Any? =
        currentPlayer.currentManifest

    override fun getCurrentTimeline(): Timeline =
        currentPlayer.currentTimeline

    override fun getCurrentPeriodIndex(): Int =
        currentPlayer.currentPeriodIndex

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun getCurrentWindowIndex(): Int =
        currentPlayer.currentWindowIndex

    override fun getCurrentMediaItemIndex(): Int =
        currentPlayer.currentMediaItemIndex

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun getNextWindowIndex(): Int =
        currentPlayer.nextWindowIndex

    override fun getNextMediaItemIndex(): Int =
        currentPlayer.nextMediaItemIndex

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun getPreviousWindowIndex(): Int =
        currentPlayer.previousWindowIndex

    override fun getPreviousMediaItemIndex(): Int =
        currentPlayer.previousMediaItemIndex

    override fun getCurrentMediaItem(): MediaItem? =
        currentPlayer.currentMediaItem

    override fun getMediaItemCount(): Int =
        currentPlayer.mediaItemCount

    override fun getMediaItemAt(index: Int): MediaItem =
        currentPlayer.getMediaItemAt(index)

    override fun getDuration(): Long =
        currentPlayer.duration

    override fun getCurrentPosition(): Long =
        currentPlayer.currentPosition

    override fun getBufferedPosition(): Long =
        currentPlayer.bufferedPosition

    override fun getBufferedPercentage(): Int =
        currentPlayer.bufferedPercentage

    override fun getTotalBufferedDuration(): Long =
        currentPlayer.totalBufferedDuration

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowDynamic(): Boolean =
        currentPlayer.isCurrentWindowDynamic

    override fun isCurrentMediaItemDynamic(): Boolean =
        currentPlayer.isCurrentMediaItemDynamic

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowLive(): Boolean =
        currentPlayer.isCurrentWindowLive

    override fun isCurrentMediaItemLive(): Boolean =
        currentPlayer.isCurrentMediaItemLive

    override fun getCurrentLiveOffset(): Long =
        currentPlayer.currentLiveOffset

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowSeekable(): Boolean =
        currentPlayer.isCurrentWindowSeekable

    override fun isCurrentMediaItemSeekable(): Boolean =
        currentPlayer.isCurrentMediaItemSeekable

    override fun isPlayingAd(): Boolean =
        currentPlayer.isPlayingAd

    override fun getCurrentAdGroupIndex(): Int =
        currentPlayer.currentAdGroupIndex

    override fun getCurrentAdIndexInAdGroup(): Int =
        currentPlayer.currentAdIndexInAdGroup

    override fun getContentDuration(): Long =
        currentPlayer.contentDuration

    override fun getContentPosition(): Long =
        currentPlayer.contentPosition

    override fun getContentBufferedPosition(): Long =
        currentPlayer.contentBufferedPosition

    override fun getAudioAttributes(): AudioAttributes =
        currentPlayer.audioAttributes

    override fun setVolume(volume: Float) {
        currentPlayer.volume = volume
    }

    override fun getVolume(): Float =
        currentPlayer.volume

    override fun clearVideoSurface() {
        currentPlayer.clearVideoSurface()
    }

    override fun clearVideoSurface(surface: Surface?) {
        currentPlayer.clearVideoSurface(surface)
    }

    override fun setVideoSurface(surface: Surface?) {
        currentPlayer.setVideoSurface(surface)
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        currentPlayer.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        currentPlayer.clearVideoSurfaceHolder(surfaceHolder)
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) =
        currentPlayer.setVideoSurfaceView(surfaceView)

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) =
        currentPlayer.clearVideoSurfaceView(surfaceView)

    override fun setVideoTextureView(textureView: TextureView?) =
        currentPlayer.setVideoTextureView(textureView)

    override fun clearVideoTextureView(textureView: TextureView?) =
        currentPlayer.clearVideoTextureView(textureView)

    override fun getVideoSize(): VideoSize =
        currentPlayer.videoSize

    @Deprecated("Deprecated in Java")
    override fun getSurfaceSize(): Size =
        currentPlayer.surfaceSize

    override fun getCurrentCues(): CueGroup =
        currentPlayer.currentCues

    override fun getDeviceInfo(): DeviceInfo =
        currentPlayer.deviceInfo

    override fun getDeviceVolume(): Int =
        currentPlayer.deviceVolume

    override fun isDeviceMuted(): Boolean =
        currentPlayer.isDeviceMuted

    override fun setDeviceVolume(volume: Int) {
        currentPlayer.deviceVolume = volume
    }

    override fun increaseDeviceVolume() {
        currentPlayer.increaseDeviceVolume()
    }

    override fun decreaseDeviceVolume() {
        currentPlayer.decreaseDeviceVolume()
    }

    override fun setDeviceMuted(muted: Boolean) {
        currentPlayer.isDeviceMuted = muted
    }
}
