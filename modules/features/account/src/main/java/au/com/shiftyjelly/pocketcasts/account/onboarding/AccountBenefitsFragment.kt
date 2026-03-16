package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.InformationalModalViewCardShowedEvent
import com.automattic.eventhorizon.InformationalModalViewDismissedEvent
import com.automattic.eventhorizon.InformationalModalViewGetStartedTapEvent
import com.automattic.eventhorizon.InformationalModalViewLoginTapEvent
import com.automattic.eventhorizon.InformationalModalViewShowedEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountBenefitsFragment : BaseDialogFragment() {
    @Inject
    internal lateinit var eventHorizon: EventHorizon

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            eventHorizon.track(InformationalModalViewShowedEvent)
        }

        AppTheme(theme.activeTheme) {
            AccountBenefitsDialog(
                onGetStartedClick = {
                    eventHorizon.track(InformationalModalViewGetStartedTapEvent)
                    dismiss()
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.LoggedOut)
                },
                onLogIn = {
                    eventHorizon.track(InformationalModalViewLoginTapEvent)
                    dismiss()
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.LoggedOut)
                },
                onShowBenefit = { benefit ->
                    eventHorizon.track(
                        InformationalModalViewCardShowedEvent(
                            card = benefit.eventHorizonValue,
                        ),
                    )
                },
                onDismiss = {
                    eventHorizon.track(InformationalModalViewDismissedEvent)
                    dismiss()
                },
            )
        }
    }
}
