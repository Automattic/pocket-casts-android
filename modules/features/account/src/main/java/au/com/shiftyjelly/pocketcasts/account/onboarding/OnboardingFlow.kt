package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusUpgradeFlow.UpgradeSource
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class OnboardingFlow(val analyticsValue: String) : Parcelable {
    @Parcelize object LoggedOut : OnboardingFlow("logged_out")
    @Parcelize object InitialOnboarding : OnboardingFlow("initial_onboarding")
    @Parcelize class PlusUpsell(override val source: UpgradeSource) : PlusFlow, OnboardingFlow("plus_upsell")
//    @Parcelize class PlusAccountUpgrade(override val source: UpgradeSource): PlusFlow, OnboardingFlow("plus_account_upgrade")
//    @Parcelize class PlusAccountUpgradeNeedsLogin(override val source: UpgradeSource): PlusFlow, OnboardingFlow("plus_account_upgrade_needs_login")

    sealed interface PlusFlow {
        val source: UpgradeSource
    }
}
