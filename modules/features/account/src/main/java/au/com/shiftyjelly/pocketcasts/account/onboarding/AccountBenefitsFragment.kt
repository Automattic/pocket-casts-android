package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountBenefitsFragment : BaseDialogFragment() {
    @Inject
    internal lateinit var tracker: AnalyticsTracker

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_SHOWED)
        }

        AppTheme(theme.activeTheme) {
            AccountBenefitsDialog(
                onGetStartedClick = {
                    tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_GET_STARTED_TAP)
                    dismiss()
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.LoggedOut)
                },
                onLogIn = {
                    tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_LOGIN_TAP)
                    dismiss()
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.LoggedOut)
                },
                onShowBenefit = { benefit ->
                    tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_CARD_SHOWED, mapOf("card" to benefit.analyticsValue))
                },
                onDismiss = {
                    tracker.track(AnalyticsEvent.INFORMATIONAL_MODAL_VIEW_DISMISSED)
                    dismiss()
                },
            )
        }
    }
}
