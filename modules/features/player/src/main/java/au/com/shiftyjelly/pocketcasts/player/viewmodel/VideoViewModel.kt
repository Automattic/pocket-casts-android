package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    // Conflated so a busy main thread never blocks the thread pushing playback state
    val playbackState: LiveData<PlaybackState> = playbackManager.playbackStateFlow
        .conflate()
        .asLiveData()

    private var hideControlsJob: Job? = null
    private var lastTimeHidingControls = 0L

    private var controlsVisibleMutable = MutableLiveData(true)
    private val sourceView = SourceView.FULL_SCREEN_VIDEO
    val controlsVisible: LiveData<Boolean> get() = controlsVisibleMutable

    fun play() {
        playbackManager.playQueue(sourceView = sourceView)
        startHideControlsTimer()
    }

    fun pause() {
        playbackManager.pause(sourceView = sourceView)
    }

    fun skipBackward() {
        playbackManager.skipBackward(sourceView = sourceView)
        startHideControlsTimer()
    }

    fun skipForward() {
        playbackManager.skipForward(sourceView = sourceView)
        startHideControlsTimer()
    }

    fun playPause() {
        if (playbackManager.isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun isPlaying(): Boolean {
        return playbackManager.isPlaying()
    }

    fun seekStarted() {
        stopHideControlsTimer()
    }

    fun seekToMs(seekTimeMs: Int) {
        playbackManager.seekToTimeMs(seekTimeMs)
        startHideControlsTimer()
    }

    fun showControls() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && System.currentTimeMillis() - lastTimeHidingControls < 200L) {
            // Avoids an issue with API 29 and below where extra calls to showControl get triggered after hiding the controls
            return
        }
        controlsVisibleMutable.value = true
        startHideControlsTimer()
    }

    fun hideControls() {
        controlsVisibleMutable.value = false
        lastTimeHidingControls = System.currentTimeMillis()
    }

    fun toggleControls() {
        val showingControls = controlsVisible.value ?: true
        if (showingControls) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun startHideControlsTimer() {
        stopHideControlsTimer()
        hideControlsJob = viewModelScope.launch {
            delay(3.seconds)
            if (playbackManager.isPlaying()) {
                hideControls()
            }
        }
    }

    private fun stopHideControlsTimer() {
        hideControlsJob?.cancel()
    }
}
