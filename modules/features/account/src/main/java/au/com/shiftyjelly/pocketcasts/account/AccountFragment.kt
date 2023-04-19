package au.com.shiftyjelly.pocketcasts.account

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentAccountBinding
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.ContinueWithGoogleButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountFragmentViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeTintedDrawable
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.observeOnce
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class AccountFragment : BaseFragment() {
    companion object {
        private const val BUTTON = "button"
        private const val SIGN_IN = "sign_in"
        private const val CREATE_ACCOUNT = "create_account"
        private val ACCOUNT_SOURCE = mapOf("source" to "account")
        fun newInstance() = AccountFragment()
    }

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private val accountViewModel: CreateAccountViewModel by activityViewModels()

    private var realBinding: FragmentAccountBinding? = null
    private val binding: FragmentAccountBinding get() = realBinding ?: throw IllegalStateException("Trying to access the binding outside of the view lifecycle.")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentAccountBinding.inflate(layoutInflater).also {
            realBinding = it
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AccountFragmentViewModel by viewModels()
        viewModel.signInState.observeOnce(
            viewLifecycleOwner,
            Observer {
                val binding = realBinding ?: return@Observer

                if (it is SignInState.SignedIn) {
                    binding.btnSignIn.isVisible = false
                    binding.lblSignIn.text = getString(LR.string.profile_alreadysignedin)
                    binding.lblSaveYourPodcasts.text = getString(LR.string.profile_alreadysignedindescription)
                    binding.imgCreateAccount.setup(view.context.getThemeTintedDrawable(IR.drawable.ic_alert_small, UR.attr.support_05))
                    binding.btnCreate.text = getString(LR.string.done)
                    binding.btnCreate.setOnClickListener { activity?.finish() }
                } else {
                    val res = if (accountViewModel.supporterInstance) LR.string.profile_supporter_description else LR.string.profile_save_your_podcasts
                    binding.lblSaveYourPodcasts.setText(res)
                }
            }
        )

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.imgCreateAccount.updateLayoutParams {
                width = 0
                height = 0
            }
        }

        binding.btnClose?.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_DISMISSED, ACCOUNT_SOURCE)
            FirebaseAnalyticsTracker.closeAccountMissingClicked()
            activity?.finish()
        }

        binding.btnContinueWithGoogle?.apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    ContinueWithGoogleButton(
                        flow = null,
                        fontSize = dimensionResource(CR.dimen.car_body2_size).value.sp,
                        includePadding = false,
                        onComplete = { activity?.finish() }
                    )
                }
            }
        }

        binding.btnCreate.setOnClickListener {
            analyticsTracker.track(
                AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
                ACCOUNT_SOURCE + mapOf(BUTTON to CREATE_ACCOUNT)
            )
            FirebaseAnalyticsTracker.createAccountClicked()
            if (view.findNavController().currentDestination?.id == R.id.accountFragment) {
                if (Util.isCarUiMode(view.context) || accountViewModel.supporterInstance) { // We can't sign up to plus on cars so skip that step
                    view.findNavController().navigate(R.id.action_accountFragment_to_createEmailFragment)
                } else {
                    view.findNavController().navigate(R.id.action_accountFragment_to_createAccountFragment)
                }
            }
        }

        binding.btnSignIn.setOnClickListener {
            analyticsTracker.track(
                AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
                ACCOUNT_SOURCE + mapOf(BUTTON to SIGN_IN)
            )
            FirebaseAnalyticsTracker.signInAccountClicked()
            if (view.findNavController().currentDestination?.id == R.id.accountFragment) {
                view.findNavController().navigate(R.id.action_accountFragment_to_signInFragment)
            }
        }
    }
}
