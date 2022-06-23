package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode

data class PlaybackState(
    val state: State = State.EMPTY,
    val isBuffering: Boolean = false,
    val isPrepared: Boolean = false,
    val isSleepTimerRunning: Boolean = false,
    val title: String = "",
    val durationMs: Int = -1,
    val positionMs: Int = 0,
    val bufferedMs: Int = 0,
    val episodeUuid: String = "",
    val podcast: Podcast? = null,
    val fileMetadata: EpisodeFileMetadata? = null,
    val embeddedArtworkPath: String? = null,
    val chapters: Chapters = Chapters(),
    val lastChangeFrom: String? = null,
    val lastErrorMessage: String? = null,
    val playbackSpeed: Double = 1.0,
    val trimMode: TrimMode = TrimMode.OFF,
    val isVolumeBoosted: Boolean = false,
    // when transientLoss is true the foreground service won't be stopped
    val transientLoss: Boolean = false
) {

    enum class State {
        EMPTY, PAUSED, PLAYING, STOPPED, ERROR
    }

    val isEmpty: Boolean
        get() = state == State.EMPTY

    val isStopped: Boolean
        get() = state == State.STOPPED

    val isPlaying: Boolean
        get() = state == State.PLAYING

    val isPaused: Boolean
        get() = state == State.PAUSED

    val isError: Boolean
        get() = state == State.ERROR
}
