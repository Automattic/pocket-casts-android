package au.com.shiftyjelly.pocketcasts.settings.onboarding

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class OnboardingFlow(val analyticsValue: String) : Parcelable {
    @Parcelize object LoggedOut : OnboardingFlow("logged_out")
    @Parcelize object InitialOnboarding : OnboardingFlow("initial_onboarding")
    @Parcelize class PlusAccountUpgrade(override val source: OnboardingUpgradeSource) : PlusFlow, OnboardingFlow("plus_account_upgrade")
    @Parcelize object PlusAccountUpgradeNeedsLogin : OnboardingFlow("plus_account_upgrade_needs_login")
    @Parcelize class PlusUpsell(
        override val source: OnboardingUpgradeSource,
        val showPatronOnly: Boolean = false,
    ) : PlusFlow, OnboardingFlow(if (showPatronOnly) "patron_upsell" else "plus_upsell")
    @Parcelize class PatronAccountUpgrade(override val source: OnboardingUpgradeSource) : PlusFlow, OnboardingFlow("patron_account_upgrade")

    sealed interface PlusFlow {
        val source: OnboardingUpgradeSource
    }
}
