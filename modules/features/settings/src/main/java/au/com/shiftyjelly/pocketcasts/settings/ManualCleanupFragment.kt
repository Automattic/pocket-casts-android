package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManualCleanupFragment : BaseFragment() {
    companion object {
        private const val CLEAN_UP_CONFIRMATION_DIALOG_TAG = "clean-up-confirmation-dialog"
        fun newInstance() = ManualCleanupFragment()
    }

    private val viewModel: ManualCleanupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            ManualCleanupPage(
                viewModel = viewModel,
                onBackPress = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setup(::deleteButtonAction)
    }

    private fun deleteButtonAction() {
        viewModel.cleanupConfirmationDialog(requireContext())
            .show(parentFragmentManager, CLEAN_UP_CONFIRMATION_DIALOG_TAG)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }
}
