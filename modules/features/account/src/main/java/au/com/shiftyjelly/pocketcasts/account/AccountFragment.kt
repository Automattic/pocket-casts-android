package au.com.shiftyjelly.pocketcasts.account

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.ContinueWithGoogleButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountFragmentViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.observeOnce
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SetupAccountButtonTappedEvent
import com.automattic.eventhorizon.SetupAccountButtonType
import com.automattic.eventhorizon.SetupAccountDismissedEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.cartheme.R as CR

@AndroidEntryPoint
class AccountFragment : BaseFragment() {
    companion object {
        fun newInstance() = AccountFragment()
    }

    @Inject lateinit var eventHorizon: EventHorizon

    private val viewModel: AccountFragmentViewModel by viewModels()
    private var isSignedIn by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            AccountPage(
                state = AccountPageState(
                    isSignedIn = isSignedIn,
                    isCar = Util.isCarUiMode(requireContext()),
                    isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
                ),
                onCloseClick = ::onCloseClick,
                onCreateClick = ::onCreateClick,
                onSignInClick = ::onSignInClick,
                continueWithGoogle = {
                    AppThemeWithBackground(theme.activeTheme) {
                        ContinueWithGoogleButton(
                            flow = OnboardingFlow.LoggedOut,
                            fontSize = dimensionResource(CR.dimen.car_body2_size).value.sp,
                            includePadding = false,
                            onComplete = { _, _ -> activity?.finish() },
                        )
                    }
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.signInState.observeOnce(viewLifecycleOwner) {
            isSignedIn = it is SignInState.SignedIn
        }
    }

    private fun onCloseClick() {
        eventHorizon.track(
            SetupAccountDismissedEvent(
                flow = OnboardingFlow.LoggedOut.analyticsValue,
            ),
        )
        activity?.finish()
    }

    private fun onCreateClick() {
        if (isSignedIn) {
            activity?.finish()
            return
        }
        eventHorizon.track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlow.LoggedOut.analyticsValue,
                button = SetupAccountButtonType.CreateAccount,
            ),
        )
        if (findNavController().currentDestination?.id == R.id.accountFragment) {
            findNavController().navigate(R.id.action_accountFragment_to_createEmailFragment)
        }
    }

    private fun onSignInClick() {
        eventHorizon.track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlow.LoggedOut.analyticsValue,
                button = SetupAccountButtonType.SignIn,
            ),
        )
        if (findNavController().currentDestination?.id == R.id.accountFragment) {
            findNavController().navigate(R.id.action_accountFragment_to_signInFragment)
        }
    }
}
