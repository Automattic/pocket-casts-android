package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.annotation.FloatRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State.Loaded.SegmentsData
import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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
        get() = storiesDataSource.numOfStories

    private var currentIndex: Int = 0
    private val nextIndex
        get() = (currentIndex.plus(1)).coerceAtMost(numOfStories.minus(1))

    private var timer: Timer? = null

    init {
        viewModelScope.launch {
            storiesDataSource.loadStories().collect { stories ->
                val state = if (stories.isEmpty()) {
                    State.Error
                } else {
                    with(storiesDataSource) {
                        State.Loaded(
                            currentStory = storyAt(currentIndex),
                            segmentsData = SegmentsData(
                                xStartOffsets = List(numOfStories) { getXStartOffsetAtIndex(it) },
                                widths = storyLengthsInMs.map { it / totalLengthInMs.toFloat() },
                            )
                        )
                    }
                }
                mutableState.value = state
                if (state is State.Loaded) start()
            }
        }
    }

    fun start() {
        val currentState = state.value as State.Loaded
        val progressFraction =
            (PROGRESS_UPDATE_INTERVAL_MS / storiesDataSource.totalLengthInMs.toFloat())
                .coerceAtMost(PROGRESS_END_VALUE)

        timer = fixedRateTimer(period = PROGRESS_UPDATE_INTERVAL_MS) {
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
        if (timer == null) start()
        mutableProgress.value = getXStartOffsetAtIndex(index)
        currentIndex = index
        mutableState.value =
            (state.value as State.Loaded).copy(currentStory = storiesDataSource.storyAt(index))
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }

    private fun Float.roundOff() = (this * 100.0).roundToInt()

    @FloatRange(from = 0.0, to = 1.0)
    fun getXStartOffsetAtIndex(index: Int): Float {
        val sumOfStoryLengthsTillIndex = try {
            storiesDataSource.storyLengthsInMs.subList(0, index).sum()
        } catch (e: IndexOutOfBoundsException) {
            Timber.e("Story offset checked at invalid index")
            0L
        }
        return (sumOfStoryLengthsTillIndex + StoriesDataSource.STORY_GAP_LENGTH_MS * index) / storiesDataSource.totalLengthInMs.toFloat()
    }

    sealed class State {
        object Loading : State()
        data class Loaded(
            val currentStory: Story?,
            val segmentsData: SegmentsData,
        ) : State() {
            data class SegmentsData(
                val widths: List<Float> = emptyList(),
                val xStartOffsets: List<Float> = emptyList(),
            )
        }
        object Error : State()
    }

    companion object {
        private const val PROGRESS_START_VALUE = 0f
        private const val PROGRESS_END_VALUE = 1f
        private const val PROGRESS_UPDATE_INTERVAL_MS = 10L
    }
}
