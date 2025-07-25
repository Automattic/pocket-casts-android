package au.com.shiftyjelly.pocketcasts.settings.onboarding

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface OnboardingFlow : Parcelable {
    val analyticsValue: String
    val source: OnboardingUpgradeSource get() = OnboardingUpgradeSource.UNKNOWN
    val preselectedTier: SubscriptionTier get() = SubscriptionTier.Plus
    val preselectedBillingCycle: BillingCycle get() = BillingCycle.Yearly

    @Parcelize
    data object LoggedOut : OnboardingFlow {
        override val analyticsValue get() = "logged_out"
    }

    @Parcelize
    data object InitialOnboarding : OnboardingFlow {
        override val analyticsValue get() = "initial_onboarding"
    }

    @Parcelize
    data object EngageSdk : OnboardingFlow {
        override val analyticsValue get() = "engage_sdk"
    }

    @Parcelize
    data class PlusAccountUpgrade(
        override val source: OnboardingUpgradeSource,
        override val preselectedTier: SubscriptionTier,
        override val preselectedBillingCycle: BillingCycle,
    ) : OnboardingFlow {
        override val analyticsValue get() = "plus_account_upgrade"
    }

    @Parcelize
    data object PlusAccountUpgradeNeedsLogin : OnboardingFlow {
        override val analyticsValue get() = "plus_account_upgrade_needs_login"
    }

    @Parcelize
    data object ReferralLoginOrSignUp : OnboardingFlow {
        override val analyticsValue get() = "referral_login_or_signup"
    }

    @Parcelize
    data class Upsell(
        override val source: OnboardingUpgradeSource,
    ) : OnboardingFlow {
        override val analyticsValue get() = "plus_upsell"
    }

    @Parcelize
    data class UpsellSuggestedFolder(
        val action: SuggestedFoldersAction,
    ) : OnboardingFlow {
        override val analyticsValue get() = "suggested_folders"
        override val source get() = OnboardingUpgradeSource.SUGGESTED_FOLDERS
    }

    @Parcelize
    data class PatronAccountUpgrade(
        override val source: OnboardingUpgradeSource,
    ) : OnboardingFlow {
        override val analyticsValue get() = "patron_account_upgrade"
    }

    @Parcelize
    data object Welcome : OnboardingFlow {
        override val analyticsValue get() = "welcome"
    }

    @Parcelize
    data object AccountEncouragement : OnboardingFlow {
        override val analyticsValue get() = "account_encouragement"
    }

    @Parcelize
    data object NewOnboardingAccountUpgrade : OnboardingFlow {
        override val analyticsValue get() = "new_onboarding_account_upgrade"
    }
}

enum class SuggestedFoldersAction {
    UseSuggestion,
    CreateCustom,
}
