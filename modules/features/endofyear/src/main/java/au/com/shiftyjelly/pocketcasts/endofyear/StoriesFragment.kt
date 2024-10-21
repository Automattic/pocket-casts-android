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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.endofyear.ui.StoriesPage
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseAppCompatDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.flow.collect
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

            StoriesPage(
                state = state,
                pagerState = pagerState,
                onUpsellClick = {},
                onClose = ::dismiss,
            )

            LaunchedEffect(state::class) {
                if (state is UiState.Synced) {
                    snapshotFlow { pagerState.currentPage }.collect { index ->
                        val stories = (state as? UiState.Synced)?.stories
                        if (stories != null) {
                            viewModel.onStoryChanged(stories[index])
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                viewModel.switchStory.collect {
                    val stories = (state as? UiState.Synced)?.stories.orEmpty()
                    if (stories.getOrNull(pagerState.currentPage) is Story.Ending) {
                        dismiss()
                    } else if (!pagerState.isScrollInProgress) {
                        pagerState.scrollToPage(pagerState.currentPage + 1)
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onDismiss(dialog)
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
