package au.com.shiftyjelly.pocketcasts.endofyear

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.endofyear.ShareableTextProvider.ShareTextData
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State.Loaded.SegmentsData
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val endOfYearManager: EndOfYearManager,
    private val fileUtilWrapper: FileUtilWrapper,
    private val shareableTextProvider: ShareableTextProvider,
    private val analyticsTracker: AnalyticsTracker,
    private val settings: Settings,
    private val subscriptionManager: SubscriptionManager,
    private val crashLogging: CrashLogging,
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

    private var progressUpdateJob: Job? = null

    private val currentStoryIsPlus: Boolean
        get() = stories.value[currentIndex].plusOnly
    private var manuallySkipped = false

    init {
        viewModelScope.launch {
            loadStories()
        }
    }

    private suspend fun CoroutineScope.loadStories() {
        try {
            val onProgressChanged: (Float) -> Unit = { progress ->
                mutableState.value = State.Loading(progress)
            }
            combine(
                subscriptionManager.freeTrialForSubscriptionTierFlow(Subscription.SubscriptionTier.PLUS),
                settings.cachedSubscriptionStatus.flow,
            ) { freeTrial, _ ->
                val currentUserTier = settings.userTier
                val lastUserTier = (state.value as? State.Loaded)?.userTier
                if (lastUserTier == currentUserTier) return@combine

                endOfYearManager.downloadListeningHistory(onProgressChanged = onProgressChanged)
                stories.value = endOfYearManager.loadStories()

                updateState(
                    freeTrial = freeTrial,
                    currentUserTier = currentUserTier,
                )
                if (state.value is State.Loaded) start()
            }.stateIn(this)
        } catch (ex: Exception) {
            val message = "Failed to load end of year stories."
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, ex, message)
            crashLogging.sendReport(ex, message = message)
            mutableState.value = State.Error
        }
    }

    private fun updateState(
        freeTrial: FreeTrial,
        currentUserTier: UserTier,
    ) {
        val state = if (stories.value.isEmpty()) {
            State.Error
        } else {
            State.Loaded(
                currentStory = stories.value[currentIndex],
                segmentsData = SegmentsData(
                    xStartOffsets = List(numOfStories) { getXStartOffsetAtIndex(it) },
                    widths = storyLengthsInMs.map { it / totalLengthInMs.toFloat() },
                ),
                userTier = currentUserTier,
                freeTrial = freeTrial,
            )
        }
        mutableState.value = state
    }

    fun start() {
        val currentState = state.value as? State.Loaded ?: return
        mutableState.value = currentState.copy(paused = false)
        val progressFraction = (PROGRESS_UPDATE_INTERVAL_MS / totalLengthInMs.toFloat()).coerceAtMost(PROGRESS_END_VALUE)

        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL_MS)
                var newProgress = (progress.value + progressFraction).coerceIn(PROGRESS_START_VALUE, PROGRESS_END_VALUE)

                if (newProgress.roundOff() == getXStartOffsetAtIndex(nextIndex).roundOff()) {
                    manuallySkipped = false
                    if (shouldSkipPlusStories()) {
                        currentIndex = nextIndex + numberOfPlusStoriesAfterTheCurrentOne()
                        newProgress = getXStartOffsetAtIndex(currentIndex)
                    } else {
                        currentIndex = nextIndex
                    }
                    mutableState.value = currentState.copy(
                        currentStory = stories.value[currentIndex],
                        paused = false,
                    )
                }

                mutableProgress.value = newProgress
                if (newProgress == PROGRESS_END_VALUE) cancelTimer()
            }
        }
    }

    fun skipPrevious() {
        val prevIndex = (currentIndex.minus(max(numberOfPlusStoriesBeforeTheCurrentOne(), 1))).coerceAtLeast(0)
        manuallySkipped = true
        skipToStoryAtIndex(prevIndex)
    }

    fun skipNext() {
        currentIndex = if (currentStoryIsPlus) {
            currentIndex + numberOfPlusStoriesAfterTheCurrentOne()
        } else {
            currentIndex
        }
        manuallySkipped = true
        skipToStoryAtIndex(nextIndex)
    }

    fun pause() {
        mutableState.value = (state.value as State.Loaded).copy(paused = true)
        cancelTimer()
    }

    fun replay() {
        analyticsTracker.track(AnalyticsEvent.END_OF_YEAR_STORY_REPLAY_BUTTON_TAPPED)
        skipToStoryAtIndex(0)
    }

    private fun skipToStoryAtIndex(index: Int) {
        if (progressUpdateJob == null) start()
        mutableProgress.value = getXStartOffsetAtIndex(index)
        currentIndex = index
        mutableState.value = (state.value as State.Loaded).copy(
            currentStory = stories.value[index],
            paused = false,
        )
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
                EOY_STORY_SAVE_FILE_NAME,
            )

            mutableState.value = currentState.copy(preparingShareText = true)

            val shareTextData = shareableTextProvider.getShareableDataForStory(story)
            mutableState.value = currentState.copy(preparingShareText = false)

            savedFile?.let { showShareForFile.invoke(it, shareTextData) }
        }
    }

    fun shouldShowUpsell() =
        currentStoryIsPlus && !isPaidUser()

    private fun numberOfPlusStoriesBeforeTheCurrentOne(): Int {
        if (isPaidUser()) return 0

        var currentStoryIndex = currentIndex
        var numberOfStoriesToSkip = 0
        while (currentStoryIndex > 0 && (stories.value[currentStoryIndex - 1]).plusOnly) {
            numberOfStoriesToSkip += 1
            currentStoryIndex -= 1
        }

        return numberOfStoriesToSkip
    }

    private fun numberOfPlusStoriesAfterTheCurrentOne(): Int {
        if (isPaidUser()) return 0

        var currentStoryIndex = currentIndex
        var numberOfStoriesToSkip = 0
        while (currentStoryIndex + 1 < numOfStories && (stories.value[currentStoryIndex + 1]).plusOnly) {
            numberOfStoriesToSkip += 1
            currentStoryIndex += 1
        }

        return numberOfStoriesToSkip
    }

    private fun isPaidUser(): Boolean {
        val currentState = state.value as? State.Loaded ?: return false
        return currentState.userTier != UserTier.Free
    }

    private fun nextStoryIsPlus() =
        if (currentIndex + 1 < numOfStories) {
            stories.value[currentIndex + 1].plusOnly
        } else {
            false
        }

    /* Whether some Plus stories should be skipped or not */
    private fun shouldSkipPlusStories() =
        !isPaidUser() && !manuallySkipped && currentStoryIsPlus && nextStoryIsPlus()

    private fun cancelTimer() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
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
            val progress: Float = 0f,
        ) : State()

        data class Loaded(
            val currentStory: Story?,
            val segmentsData: SegmentsData,
            val preparingShareText: Boolean = false,
            val userTier: UserTier,
            val freeTrial: FreeTrial,
            val paused: Boolean = false,
        ) : State() {
            data class SegmentsData(
                val widths: List<Float> = emptyList(),
                val xStartOffsets: List<Float> = emptyList(),
            )
        }

        object Error : State()
    }

    fun trackStoryOrUpsellShown() {
        val currentState = state.value as State.Loaded
        val currentStory = requireNotNull(currentState.currentStory)
        val event = if (shouldShowUpsell()) {
            AnalyticsEvent.END_OF_YEAR_UPSELL_SHOWN
        } else {
            AnalyticsEvent.END_OF_YEAR_STORY_SHOWN
        }
        analyticsTracker.track(
            event,
            AnalyticsProp.storyShown(currentStory.identifier),
        )
    }

    fun trackStoryShared() {
        val currentState = state.value as? State.Loaded
        analyticsTracker.track(
            AnalyticsEvent.END_OF_YEAR_STORY_SHARED,
            AnalyticsProp.storyShared(
                currentState?.currentStory?.identifier ?: "",
                shareableTextProvider.chosenActivity ?: "",
            ),
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
