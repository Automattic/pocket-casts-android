package au.com.shiftyjelly.pocketcasts.appreview

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.settings.HelpFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppReviewDialogFragment : BaseDialogFragment() {
    private val viewModel by viewModels<AppReviewViewModel>()

    private var willReview = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            AppReviewPage(
                onClickNotReally = ::goToHelpAndFeedback,
                onClickYes = {
                    willReview = true
                    dismiss()
                },
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            )
        }
    }

    private fun goToHelpAndFeedback() {
        val hostListener = requireActivity() as FragmentHostListener
        hostListener.addFragment(HelpFragment())
        hostListener.closeBottomSheet()
        hostListener.closePlayer()
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!requireActivity().isChangingConfigurations && !willReview) {
            viewModel.declineAppReview()
        }
    }
}
