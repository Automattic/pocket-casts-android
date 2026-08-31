package au.com.shiftyjelly.pocketcasts.account.deviceapprove

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceApproveFragment : BaseDialogFragment() {

    private val viewModel by viewModels<DeviceApproveViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setUserCode(arguments?.getString(ARG_USER_CODE).orEmpty())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val state by viewModel.uiState.collectAsState()

        AppTheme(theme.activeTheme) {
            DeviceApprovePage(
                state = state,
                onConnect = viewModel::connect,
                onSetUpAccount = {
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.DeviceApproval)
                },
                onDone = ::finishAfterApproval,
                onClose = ::dismiss,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccountState()
    }

    private fun finishAfterApproval() {
        val activity = requireActivity()
        val promptUpsell = viewModel.shouldPromptUpsellAfterApproval
        dismiss()
        if (promptUpsell) {
            OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.LOGIN))
        }
    }

    companion object {
        private const val ARG_USER_CODE = "user_code"

        fun newInstance(userCode: String) = DeviceApproveFragment().apply {
            arguments = Bundle().apply { putString(ARG_USER_CODE, userCode) }
        }
    }
}
