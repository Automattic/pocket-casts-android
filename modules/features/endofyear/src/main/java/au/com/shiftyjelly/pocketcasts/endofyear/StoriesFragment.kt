package au.com.shiftyjelly.pocketcasts.endofyear

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            StoriesPage(
                state = state,
                onClose = ::dismiss,
            )
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
