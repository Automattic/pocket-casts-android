package au.com.shiftyjelly.pocketcasts.appreview

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import au.com.shiftyjelly.pocketcasts.settings.HelpFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.play.core.review.ReviewInfo
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class AppReviewDialogFragment : BaseDialogFragment() {
    private val viewModel by viewModels<AppReviewViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AppReviewViewModel.Factory> { factory ->
                factory.create(args.reason)
            }
        },
    )

    private var userReviewResponse: UserReviewResponse? = null

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_KEY)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            viewModel.trackPageShown()
        }

        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            AppReviewPage(
                onClickNotReally = {
                    userReviewResponse = UserReviewResponse.No
                    viewModel.trackNoResponse()
                    goToHelpAndFeedback()
                },
                onClickYes = {
                    userReviewResponse = UserReviewResponse.Yes
                    viewModel.trackYesResponse()
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.launchReview(requireActivity(), args.reviewInfo)
                        dismiss()
                    }
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
        if (!requireActivity().isChangingConfigurations) {
            when (userReviewResponse) {
                UserReviewResponse.Yes -> Unit

                UserReviewResponse.No -> {
                    viewModel.declineAppReview()
                }

                null -> {
                    viewModel.trackPageDismissed()
                    viewModel.declineAppReview()
                }
            }
        }
        super.onDismiss(dialog)
    }

    @Parcelize
    private class Args(
        val reason: AppReviewReason,
        val reviewInfo: ReviewInfo,
    ) : Parcelable

    companion object {
        const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(
            reason: AppReviewReason,
            reviewInfo: ReviewInfo,
        ) = AppReviewDialogFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_KEY to Args(reason, reviewInfo))
        }
    }
}

private enum class UserReviewResponse {
    No,
    Yes,
}
