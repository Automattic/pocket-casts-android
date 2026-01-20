package au.com.shiftyjelly.pocketcasts.endofyear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.endofyear.ui.ScreenshotDetectedDialog
import au.com.shiftyjelly.pocketcasts.endofyear.ui.StoriesPage
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.ScreenshotCaptureDetector
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class StoriesActivity : ComponentActivity() {
    private val source: StoriesSource
        get() = requireNotNull(IntentCompat.getSerializableExtra(intent, ARG_SOURCE, StoriesSource::class.java))
    private lateinit var screenshotDetector: ScreenshotCaptureDetector
    private val screenshotDetectedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val isTablet get() = Util.isTablet(this)

    private val viewModel by viewModels<EndOfYearViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<EndOfYearViewModel.Factory> { factory ->
                val year = EndOfYearManager.YEAR_TO_SYNC
                factory.create(
                    year = year,
                    topListTitle = getString(LR.string.end_of_year_story_top_podcasts_list_title, year.value),
                    source = source,
                )
            }
        },
    )

    @Inject
    lateinit var settings: Settings

    private val onboardingLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(OnboardingActivityContract()) { result ->
        when (result) {
            is OnboardingFinish.Done, is OnboardingFinish.DoneGoToDiscover -> {
                settings.setHasDoneInitialOnboarding()
            }

            is OnboardingFinish.DoneShowPlusPromotion -> {
                settings.setHasDoneInitialOnboarding()
                // Disabled - only show Plus prompts when user-initiated
                // OnboardingLauncher.openOnboardingFlow(this, OnboardingFlow.Upsell(OnboardingUpgradeSource.LOGIN_PLUS_PROMOTION))
            }

            is OnboardingFinish.DoneShowWelcomeInReferralFlow -> {
                settings.showReferralWelcome.set(true, updateModifiedAt = false)
            }

            is OnboardingFinish.DoneApplySuggestedFolders, null -> {
                Timber.e("Unexpected result $result from onboarding activity")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTablet) {
            enableEdgeToEdge(SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
            hideSystemBars()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (savedInstanceState == null) {
            viewModel.trackStoriesShown()
        }
        screenshotDetector = ScreenshotCaptureDetector.create(this) {
            screenshotDetectedFlow.tryEmit(Unit)
        }
        setContent { Content() }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            val isNavigationBarVisible = windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
            val isStatusBarVisible = windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
            if (isNavigationBarVisible || isStatusBarVisible) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
            ViewCompat.onApplyWindowInsets(view, windowInsets)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Content() {
        LaunchedEffect(Unit) {
            viewModel.syncData()
        }

        val scope = rememberCoroutineScope()
        val state by viewModel.uiState.collectAsState()
        val pagerState = rememberPagerState(pageCount = { state.stories.size })
        val storyChanger = remember(pagerState, scope) {
            StoryChanger(pagerState, viewModel, scope)
        }
        val captureController = rememberStoryCaptureController()
        var showScreenshotDialog by remember { mutableStateOf(false) }

        BackHandler {
            val story = state.stories.getOrNull(pagerState.currentPage)
            viewModel.trackStoriesClosed(source = "back_button", story = story)
            finish()
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize(),
        ) {
            StoriesPage(
                state = state,
                pagerState = pagerState,
                controller = captureController,
                insets = if (isTablet) WindowInsets(top = 16.dp) else WindowInsets.statusBarsIgnoringVisibility,
                onChangeStory = storyChanger::change,
                onShareStory = ::shareStory,
                onHoldStory = { viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.UserHoldingStory) },
                onReleaseStory = { viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.UserHoldingStory) },
                onLearnAboutRatings = ::openRatingsInfo,
                onClickUpsell = ::startUpsellFlow,
                onClickPlusContinue = viewModel::trackPlusContinued,
                onRestartPlayback = storyChanger::reset,
                onClose = {
                    val story = state.stories.getOrNull(pagerState.currentPage)
                    viewModel.trackStoriesClosed(source = "close_button", story = story)
                    finish()
                },
            )
        }

        if (showScreenshotDialog) {
            ScreenshotDetectedDialog(
                onNotNow = {
                    showScreenshotDialog = false
                    viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenshotDialog)
                },
                onShare = {
                    showScreenshotDialog = false
                    viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenshotDialog)
                    val story = state.stories.getOrNull(pagerState.currentPage)
                    if (story != null) {
                        scope.launch {
                            val screenshot = captureController.capture(story)
                            if (screenshot != null) {
                                viewModel.share(story, screenshot)
                            }
                        }
                    }
                },
            )
        }

        LaunchedEffect(Unit) {
            viewModel.switchStory.collect {
                val story = state.stories.getOrNull(pagerState.currentPage)
                if (story is Story.Ending) {
                    viewModel.trackStoriesAutoFinished(story = story)
                    finish()
                } else {
                    storyChanger.change(moveForward = true)
                }
            }
        }

        var lastStory by remember { mutableStateOf<Story?>(null) }
        LaunchedEffect(state::class) {
            if (state is UiState.Synced || state is UiState.Syncing) {
                // Track displayed page to not report it twice from different events.
                // This can happen, for example, after the first launch.
                // Both currentPage and pageCount trigger an event when the pager is set up.
                // Inform VM about a story changed due to explicit changes of the current page.
                launch {
                    snapshotFlow { pagerState.currentPage }.collect { index ->
                        val newStory = state.stories.getOrNull(index)
                        if (newStory != null && lastStory != newStory) {
                            lastStory = newStory
                            viewModel.onStoryChanged(newStory)
                        }
                    }
                }
                // Inform VM about a story changed due to a change in the stories list
                // This happens when a user successfully upgrades their account.
                launch {
                    snapshotFlow { pagerState.pageCount }.collect {
                        val newStory = state.stories.getOrNull(pagerState.currentPage)
                        if (newStory != null && lastStory != newStory) {
                            lastStory = newStory
                            viewModel.onStoryChanged(newStory)
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            screenshotDetectedFlow.collectLatest {
                val currentStory = state.stories.getOrNull(pagerState.currentPage)
                if (currentStory?.isShareable == true) {
                    viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.ScreenshotDialog)
                    viewModel.screenshotDetected(story = currentStory, activity = this@StoriesActivity)
                    showScreenshotDialog = true
                }
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { captureController.isSharing }.collect { isSharing ->
                if (isSharing) {
                    viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.TakingScreenshot)
                } else {
                    viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.TakingScreenshot)
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.syncFailedSignal.await()
            setResult(0, StoriesActivityContract.setResult(source))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
        screenshotDetector.register()
    }

    override fun onPause() {
        screenshotDetector.unregister()
        // Pause auto progress when the fragment is not active.
        // This makes sure that users see all stories and they
        // won't auto switch for example when signing up takes
        // some time or when the EoY flow is interruped by
        // some other user actions such as a phone call.
        viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
        super.onPause()
    }

    private fun startUpsellFlow() {
        return // Disabled automatic upsell - only show Plus prompts when user-initiated
        @Suppress("UNREACHABLE_CODE")
        viewModel.trackUpsellShown()
        val flow = OnboardingFlow.Upsell(OnboardingUpgradeSource.END_OF_YEAR)
        onboardingLauncher.launch(OnboardingActivity.newInstance(this, flow))
    }

    private fun openRatingsInfo() {
        viewModel.trackLearnRatingsShown()
        WebViewActivity.show(
            this,
            getString(LR.string.podcast_ratings_page_title),
            "https://support.pocketcasts.com/knowledge-base/ratings/",
        )
    }

    private fun shareStory(story: Story, file: File) {
        viewModel.share(story, file)
    }

    enum class StoriesSource(val value: String) {
        MODAL("modal"),
        PROFILE("profile"),
        USER_LOGIN("user_login"),
        UNKNOWN("unknown"),
        ;

        companion object {
            fun fromString(source: String) = entries.find { it.value == source } ?: UNKNOWN
        }
    }

    companion object {
        private const val ARG_SOURCE = "source"

        fun intent(activity: Activity, source: StoriesSource) = Intent(activity, StoriesActivity::class.java)
            .putExtra(ARG_SOURCE, source)

        fun open(activity: Activity, source: StoriesSource) {
            val intent = intent(activity, source)
            activity.startActivity(intent)
        }
    }

    class StoriesActivityContract : ActivityResultContract<Intent, StoriesActivityContract.Result>() {

        sealed class Result : Parcelable {
            @Parcelize
            data class Failure(val source: StoriesSource?) : Result()

            @Parcelize
            data object Success : Result()
        }

        companion object {
            private const val EXTRA_SOURCE = "stories_source"

            fun setResult(source: StoriesSource) = Intent().apply {
                putExtra(EXTRA_SOURCE, source)
            }
        }

        override fun createIntent(context: Context, input: Intent) = input

        override fun parseResult(resultCode: Int, intent: Intent?): Result {
            return if (intent == null || resultCode == RESULT_OK) {
                Result.Success
            } else {
                Result.Failure(source = IntentCompat.getParcelableExtra(intent, EXTRA_SOURCE, StoriesSource::class.java))
            }
        }
    }
}

private class StoryChanger(
    private val pagerState: PagerState,
    private val viewModel: EndOfYearViewModel,
    private val scope: CoroutineScope,
) {
    fun change(moveForward: Boolean) {
        if (!pagerState.isScrollInProgress) {
            val currentPage = pagerState.currentPage
            val nextIndex = if (moveForward) {
                viewModel.getNextStoryIndex(currentPage)
            } else {
                viewModel.getPreviousStoryIndex(currentPage)
            }
            if (nextIndex != null) {
                scope.launch { pagerState.scrollToPage(nextIndex) }
            }
        }
    }

    fun reset() {
        viewModel.trackReplayStoriesTapped()
        scope.launch { pagerState.scrollToPage(0) }
    }
}
