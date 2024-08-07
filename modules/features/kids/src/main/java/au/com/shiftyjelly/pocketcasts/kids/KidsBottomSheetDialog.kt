package au.com.shiftyjelly.pocketcasts.kids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.kids.viewmodel.KidsSendFeedbackViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KidsBottomSheetDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var theme: Theme

    private val viewModel: KidsSendFeedbackViewModel by viewModels()

    private val animationSpec = tween<IntSize>(
        durationMillis = 400,
        easing = EaseInOut,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsState()

                AppTheme(theme.activeTheme) {
                    AnimatedVisibility(
                        visible = showFeedbackDialog,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = animationSpec,
                        ),
                    ) {
                        KidsSendFeedbackDialog(
                            onSeen = viewModel::onFeedbackFormSeen,
                            onSubmitFeedback = {
                                viewModel.onSubmitFeedback()
                                dismiss()
                            },
                        )
                    }
                    AnimatedVisibility(
                        visible = !showFeedbackDialog,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = animationSpec,
                        ),
                    ) {
                        KidsDialog(
                            onSeen = viewModel::onThankYouForYourInterestSeen,
                            onSendFeedbackClick = {
                                viewModel.onSendFeedbackClick()
                            },
                            onNoThankYouClick = {
                                viewModel.onNoThankYouClick()
                                dismiss()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).run {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    peekHeight = 0
                    skipCollapsed = true
                }
            }
        }
    }
}
