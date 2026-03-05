package au.com.shiftyjelly.pocketcasts.settings.onboarding

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.automattic.eventhorizon.OnboardingFlowType
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface OnboardingFlow : Parcelable {
    val eventHorizonValue: OnboardingFlowType
    val analyticsValue: String get() = eventHorizonValue.toString()
    val source: OnboardingUpgradeSource get() = OnboardingUpgradeSource.UNKNOWN
    val preselectedTier: SubscriptionTier get() = SubscriptionTier.Plus
    val preselectedBillingCycle: BillingCycle get() = BillingCycle.Yearly

    @Parcelize
    data object LoggedOut : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.LoggedOut
    }

    @Parcelize
    data object InitialOnboarding : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.InitialOnboarding
    }

    @Parcelize
    data object EngageSdk : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.EngageSdk
    }

    @Parcelize
    data class PlusAccountUpgrade(
        override val source: OnboardingUpgradeSource,
        override val preselectedTier: SubscriptionTier,
        override val preselectedBillingCycle: BillingCycle,
    ) : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.PlusAccountUpgrade
    }

    @Parcelize
    data object PlusAccountUpgradeNeedsLogin : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.PlusAccountUpgradeNeedsLogin
    }

    @Parcelize
    data object ReferralLoginOrSignUp : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.ReferralLoginOrSignup
    }

    @Parcelize
    data class Upsell(
        override val source: OnboardingUpgradeSource,
    ) : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.PlusUpsell
    }

    @Parcelize
    data class UpsellSuggestedFolder(
        val action: SuggestedFoldersAction,
    ) : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.SuggestedFolders
        override val source get() = OnboardingUpgradeSource.SUGGESTED_FOLDERS
    }

    @Parcelize
    data class PatronAccountUpgrade(
        override val source: OnboardingUpgradeSource,
    ) : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.PatronAccountUpgrade
    }

    @Parcelize
    data object Welcome : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.Welcome
    }

    @Parcelize
    data object AccountEncouragement : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.AccountEncouragement
    }

    @Parcelize
    data object AccountUpgrade : OnboardingFlow {
        override val eventHorizonValue get() = OnboardingFlowType.PlusAccountUpgrade
        override val source get() = OnboardingUpgradeSource.PROFILE
    }
}

enum class SuggestedFoldersAction {
    UseSuggestion,
    CreateCustom,
}
