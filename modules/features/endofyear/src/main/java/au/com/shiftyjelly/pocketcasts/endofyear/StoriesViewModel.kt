package au.com.shiftyjelly.pocketcasts.endofyear

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.endofyear.ShareableTextProvider.ShareTextData
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State.Loaded.SegmentsData
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val endOfYearManager: EndOfYearManager,
    private val fileUtilWrapper: FileUtilWrapper,
    private val shareableTextProvider: ShareableTextProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val settings: Settings,
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Loading())
    val state: StateFlow<State> = mutableState

    private val mutableProgress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = mutableProgress

    private val stories = MutableStateFlow(emptyList<Story>())
    private val numOfStories: Int
        get() = stories.value.size

    private var currentIndex: Int = 0
    private val nextIndex
        get() = (currentIndex.plus(1)).coerceAtMost(numOfStories.minus(1))
    private val totalLengthInMs
        get() = storyLengthsInMs.sum() + gapLengthsInMs
    private val storyLengthsInMs: List<Long>
        get() = stories.value.map { it.storyLength }
    private val gapLengthsInMs: Long
        get() = STORY_GAP_LENGTH_MS * numOfStories.minus(1).coerceAtLeast(0)

    private var timer: Timer? = null

    init {
        viewModelScope.launch {
            loadStories()
        }
    }

    private suspend fun loadStories() {
        try {
            val onProgressChanged: (Float) -> Unit = { progress ->
                mutableState.value = State.Loading(progress)
            }
            endOfYearManager.downloadListeningHistory(onProgressChanged = onProgressChanged)
            stories.value = endOfYearManager.loadStories()
            val state = if (stories.value.isEmpty()) {
                State.Error
            } else {
                State.Loaded(
                    currentStory = stories.value[currentIndex],
                    segmentsData = SegmentsData(
                        xStartOffsets = List(numOfStories) { getXStartOffsetAtIndex(it) },
                        widths = storyLengthsInMs.map { it / totalLengthInMs.toFloat() },
                    ),
                    userTier = settings.userTier,
                )
            }
            mutableState.value = state
            if (state is State.Loaded) start()
        } catch (ex: Exception) {
            val message = "Failed to load end of year stories."
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, ex, message)
            SentryHelper.recordException(message, ex)
            mutableState.value = State.Error
        }
    }

    fun start() {
        if (state.value !is State.Loaded) return

        val currentState = state.value as State.Loaded
        val progressFraction =
            (PROGRESS_UPDATE_INTERVAL_MS / totalLengthInMs.toFloat())
                .coerceAtMost(PROGRESS_END_VALUE)

        timer?.cancel()
        timer = fixedRateTimer(period = PROGRESS_UPDATE_INTERVAL_MS) {
            viewModelScope.launch {
                val newProgress = (progress.value + progressFraction)
                    .coerceIn(PROGRESS_START_VALUE, PROGRESS_END_VALUE)

                if (newProgress.roundOff() == getXStartOffsetAtIndex(nextIndex).roundOff()) {
                    currentIndex = nextIndex
                    mutableState.value =
                        currentState.copy(currentStory = stories.value[currentIndex])
                }

                mutableProgress.value = newProgress
                if (newProgress == PROGRESS_END_VALUE) cancelTimer()
            }
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

    fun replay() {
        analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORY_REPLAY_BUTTON_TAPPED)
        skipToStoryAtIndex(0)
    }

    private fun skipToStoryAtIndex(index: Int) {
        if (timer == null) start()
        mutableProgress.value = getXStartOffsetAtIndex(index)
        currentIndex = index
        mutableState.value = (state.value as State.Loaded).copy(currentStory = stories.value[index])
    }

    fun onRetryClicked() {
        viewModelScope.launch {
            analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORY_RETRY_BUTTON_TAPPED)
            loadStories()
        }
    }

    fun onShareClicked(
        onCaptureBitmap: () -> Bitmap,
        context: Context,
        showShareForFile: (File, ShareTextData) -> Unit,
    ) {
        pause()
        val currentState = state.value as State.Loaded
        val story = requireNotNull(currentState.currentStory)
        analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORY_SHARE, AnalyticsProp.storyShare(story.identifier))
        viewModelScope.launch {
            val savedFile = fileUtilWrapper.saveBitmapToFile(
                onCaptureBitmap.invoke(),
                context,
                EOY_STORY_SAVE_FOLDER_NAME,
                EOY_STORY_SAVE_FILE_NAME
            )

            mutableState.value = currentState.copy(preparingShareText = true)

            val shareTextData = shareableTextProvider.getShareableDataForStory(story)
            mutableState.value = currentState.copy(preparingShareText = false)

            savedFile?.let { showShareForFile.invoke(it, shareTextData) }
        }
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
            storyLengthsInMs.subList(0, index).sum()
        } catch (e: IndexOutOfBoundsException) {
            Timber.e("Story offset checked at invalid index")
            0L
        }
        return (sumOfStoryLengthsTillIndex + STORY_GAP_LENGTH_MS * index) / totalLengthInMs.toFloat()
    }

    sealed class State {
        data class Loading(
            val progress: Float = 0f
        ) : State()
        data class Loaded(
            val currentStory: Story?,
            val segmentsData: SegmentsData,
            val preparingShareText: Boolean = false,
            val userTier: UserTier,
        ) : State() {
            data class SegmentsData(
                val widths: List<Float> = emptyList(),
                val xStartOffsets: List<Float> = emptyList(),
            )
        }

        object Error : State()
    }

    fun trackStoryShown() {
        val currentState = state.value as State.Loaded
        val currentStory = requireNotNull(currentState.currentStory)
        analyticsTracker.track(
            AnalyticsEvent.END_OF_YEAR_STORY_SHOWN,
            AnalyticsProp.storyShown(currentStory.identifier)
        )
    }

    fun trackStoryShared() {
        val currentState = state.value as? State.Loaded
        analyticsTracker.track(
            AnalyticsEvent.END_OF_YEAR_STORY_SHARED,
            AnalyticsProp.storyShared(
                currentState?.currentStory?.identifier ?: "",
                shareableTextProvider.chosenActivity ?: ""
            )
        )
    }

    fun trackStoryFailedToLoad() {
        analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORIES_FAILED_TO_LOAD)
    }

    private object AnalyticsProp {
        private const val story = "story"
        private const val activity = "activity"
        fun storyShown(storyId: String) = mapOf(story to storyId)
        fun storyShare(storyId: String) = mapOf(story to storyId)
        fun storyShared(storyId: String, activityId: String) = mapOf(story to storyId, activity to activityId)
    }

    companion object {
        private const val STORY_GAP_LENGTH_MS = 500L
        private const val PROGRESS_START_VALUE = 0f
        private const val PROGRESS_END_VALUE = 1f
        private const val PROGRESS_UPDATE_INTERVAL_MS = 10L
        private const val EOY_STORY_SAVE_FOLDER_NAME = "eoy_images_cache"
        private const val EOY_STORY_SAVE_FILE_NAME = "eoy_shared_image.png"
    }
}
