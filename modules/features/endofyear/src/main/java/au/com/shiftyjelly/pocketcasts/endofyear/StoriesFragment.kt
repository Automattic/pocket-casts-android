package au.com.shiftyjelly.pocketcasts.endofyear

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.endofyear.ui.StoriesPage
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseAppCompatDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.R as AndroidR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class StoriesFragment : BaseAppCompatDialogFragment() {
    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(Color.BLACK, true)
    private val source: StoriesSource
        get() = requireNotNull(BundleCompat.getSerializable(requireArguments(), ARG_SOURCE, StoriesSource::class.java))

    private val viewModel by viewModels<EndOfYearViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<EndOfYearViewModel.Factory> { factory ->
                factory.create(EndOfYearManager.YEAR_TO_SYNC)
            }
        },
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.attributes?.windowAnimations = UR.style.WindowAnimationSlideTransition
        return dialog
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        val isTablet = Util.isTablet(requireContext())
        if (!isTablet) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            setStyle(STYLE_NORMAL, AndroidR.style.Theme_Material_NoActionBar)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            LaunchedEffect(Unit) {
                viewModel.syncData()
            }
            val state by viewModel.uiState.collectAsState()
            val pagerState = rememberPagerState(pageCount = { (state as? UiState.Synced)?.stories?.size ?: 0 })
            val storyChanger = rememberStoryChanger(pagerState, viewModel)

            StoriesPage(
                state = state,
                pagerState = pagerState,
                onChangeStory = storyChanger::change,
                onClickUpsell = ::startUpsellFlow,
                onClose = ::dismiss,
            )

            LaunchedEffect(Unit) {
                viewModel.switchStory.collect {
                    val stories = (state as? UiState.Synced)?.stories.orEmpty()
                    if (stories.getOrNull(pagerState.currentPage) is Story.Ending) {
                        dismiss()
                    } else {
                        storyChanger.change(moveForward = true)
                    }
                }
            }

            LaunchedEffect(state::class) {
                if (state is UiState.Synced) {
                    // Track displayed page to not report it twice from different events.
                    // This can happen, for example, after the first launch.
                    // Both currentPage and pageCount trigger an event when the pager is set up.
                    var lastStory: Story? = null
                    // Inform VM about a story changed due to explicit changes of the current page.
                    launch {
                        snapshotFlow { pagerState.currentPage }.collect { index ->
                            val stories = (state as? UiState.Synced)?.stories
                            val newStory = stories?.getOrNull(index)
                            if (newStory != null && lastStory != newStory) {
                                lastStory = newStory
                                viewModel.onStoryChanged(newStory)
                            }
                        }
                    }
                    // Inform VM about a story changed due to a change in the stories list
                    // This happens when a user sucessfully upgrades their account.
                    launch {
                        snapshotFlow { pagerState.pageCount }.collect {
                            val stories = (state as? UiState.Synced)?.stories
                            val newStory = stories?.getOrNull(pagerState.currentPage)
                            if (newStory != null && lastStory != newStory) {
                                lastStory = newStory
                                viewModel.onStoryChanged(newStory)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onDismiss(dialog)
    }

    private fun startUpsellFlow() {
        val flow = OnboardingFlow.Upsell(OnboardingUpgradeSource.END_OF_YEAR)
        OnboardingLauncher.openOnboardingFlow(requireActivity(), flow)
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeStoryAutoProgress()
    }

    override fun onPause() {
        // Pause auto progress when the fragment is not active.
        // This makes sure that users see all stories and they
        // won't auto switch for example when signing up takes
        // some time or when the EoY flow is interruped by
        // some other user actions such as a phone call.
        viewModel.pauseStoryAutoProgress()
        super.onPause()
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

        fun newInstance(source: StoriesSource) = StoriesFragment().apply {
            arguments = bundleOf(
                ARG_SOURCE to source,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberStoryChanger(
    pagerState: PagerState,
    viewModel: EndOfYearViewModel,
): StoryChanger {
    val scope = rememberCoroutineScope()
    return remember(pagerState) { StoryChanger(pagerState, viewModel, scope) }
}

@OptIn(ExperimentalFoundationApi::class)
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
}
