package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val storiesDataSource: StoriesDataSource,
) : ViewModel() {
    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = mutableState

    private val mutableProgress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = mutableProgress

    private val numOfStories: Int
        get() = storiesDataSource.stories.size

    private var currentIndex: Int = 0
    private val nextIndex
        get() = (currentIndex.plus(1)).coerceAtMost(numOfStories.minus(1))

    private var timer: Timer? = null
    private var timerCancelled = false

    init {
        val stories = storiesDataSource.loadStories()
        val state = if (stories.isEmpty()) {
            State.Error
        } else {
            State.Loaded(
                currentStory = storiesDataSource.storyAt(currentIndex),
                numberOfStories = numOfStories
            )
        }
        mutableState.value = state
        if (state is State.Loaded) start()
    }

    fun start() {
        val currentState = state.value as State.Loaded
        val progressFraction =
            (PROGRESS_UPDATE_INTERVAL_MS / storiesDataSource.totalLengthInMs.toFloat())
                .coerceAtMost(PROGRESS_END_VALUE)

        timer = fixedRateTimer(period = PROGRESS_UPDATE_INTERVAL_MS) {
            timerCancelled = false
            val newProgress = (progress.value + progressFraction)
                .coerceIn(PROGRESS_START_VALUE, PROGRESS_END_VALUE)

            if (newProgress.roundOff() == getXStartOffsetAtIndex(nextIndex).roundOff()) {
                currentIndex = nextIndex
                mutableState.value =
                    currentState.copy(currentStory = storiesDataSource.storyAt(currentIndex))
            }

            mutableProgress.value = newProgress
            if (newProgress == PROGRESS_END_VALUE) cancelTimer()
        }
    }

    fun skipPrevious() {
        val prevIndex = (currentIndex.minus(1)).coerceAtLeast(0)
        skipToStoryAtIndex(prevIndex)
    }

    fun skipNext() {
        skipToStoryAtIndex(nextIndex)
    }

    fun pause() {
        cancelTimer()
    }

    private fun skipToStoryAtIndex(index: Int) {
        if (timerCancelled) start()
        mutableProgress.value = getXStartOffsetAtIndex(index)
        mutableState.value =
            (state.value as State.Loaded).copy(currentStory = storiesDataSource.storyAt(index))
    }

    private fun cancelTimer() {
        timer?.cancel()
        timerCancelled = true
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }

    private fun Float.roundOff() = (this * 100.0).roundToInt()

    private fun getXStartOffsetAtIndex(index: Int) =
        (PROGRESS_END_VALUE / numOfStories.toFloat()).coerceAtMost(PROGRESS_END_VALUE) * index

    sealed class State {
        object Loading : State()
        data class Loaded(
            val currentStory: Story?,
            val numberOfStories: Int,
        ) : State()

        object Error : State()
    }

    companion object {
        private const val PROGRESS_START_VALUE = 0f
        private const val PROGRESS_END_VALUE = 1f
        private const val PROGRESS_UPDATE_INTERVAL_MS = 10L
    }
}
