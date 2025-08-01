package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingSubscriptionPlan
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.ProfileUpgradeBannerState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.NewsletterSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.winback.WinbackInitParams
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.toDurationFromNow
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class AccountDetailsViewModel @Inject constructor(
    userManager: UserManager,
    private val paymentClient: PaymentClient,
    private val syncManager: SyncManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val crashLogging: CrashLogging,
) : ViewModel() {
    internal val deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Empty)
    internal val selectedFeatureCard = MutableStateFlow<SubscriptionPlan.Key?>(null)
    internal val signInState = userManager.getSignInState()
    internal val marketingOptIn = settings.marketingOptIn.flow

    internal val headerState = signInState.asFlow().map { state ->
        when (state) {
            is SignInState.SignedOut -> AccountHeaderState.empty()
            is SignInState.SignedIn -> {
                val subscription = state.subscription
                AccountHeaderState(
                    email = state.email,
                    imageUrl = Gravatar.getUrl(state.email),
                    subscription = if (subscription == null) {
                        SubscriptionHeaderState.Free
                    } else if (subscription.isAutoRenewing) {
                        SubscriptionHeaderState.PaidRenew(
                            tier = subscription.tier,
                            expiresIn = subscription.expiryDate.toDurationFromNow(),
                            billingCycle = subscription.billingCycle,
                        )
                    } else {
                        SubscriptionHeaderState.PaidCancel(
                            tier = subscription.tier,
                            expiresIn = subscription.expiryDate.toDurationFromNow(),
                            isChampion = subscription.isChampion,
                            platform = subscription.platform,
                            giftDaysLeft = subscription.giftDays,
                        )
                    },
                )
            }
        }
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = AccountHeaderState.empty())

    internal val upgradeBannerState = combine(
        signInState.asFlow(),
        selectedFeatureCard,
    ) { signInState, featureCard ->
        val signedInState = signInState as? SignInState.SignedIn
        val isExpiring = signedInState?.subscription?.isExpiring == true

        val subscriptionPlans = paymentClient.loadSubscriptionPlans().getOrNull()
        return@combine if (subscriptionPlans == null) {
            null
        } else {
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                if (signedInState?.subscription != null) {
                    null
                } else {
                    val recommendedPlan = subscriptionPlans.findOfferPlan(tier = SubscriptionTier.Plus, BillingCycle.Yearly, offer = SubscriptionOffer.Trial).getOrNull()?.let {
                        OnboardingSubscriptionPlan.create(plan = it).getOrNull()
                    } ?: OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(tier = SubscriptionTier.Plus, billingCycle = BillingCycle.Yearly))
                    ProfileUpgradeBannerState.NewOnboardingUpgradeState(
                        recommendedSubscription = recommendedPlan,
                    )
                }
            } else {
                ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
                    subscriptionPlans = subscriptionPlans,
                    selectedFeatureCard = featureCard,
                    currentSubscription = signedInState?.subscription?.let { subscription ->
                        SubscriptionPlan.Key(
                            tier = subscription.tier,
                            billingCycle = subscription.billingCycle ?: return@let null,
                            offer = null,
                        )
                    },
                    isRenewingSubscription = isExpiring,
                )
            }
        }
            .takeIf { signInState.isSignedInAsFree || isExpiring }
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    internal val sectionsState = combine(
        signInState.asFlow(),
        marketingOptIn,
    ) { signInState, marketingOptIn ->
        val signedInState = signInState as? SignInState.SignedIn
        val isGiftExpiring = signedInState?.subscription?.isExpiring == true
        AccountSectionsState(
            isSubscribedToNewsLetter = marketingOptIn,
            email = signedInState?.email,
            winbackInitParams = computeWinbackParams(signInState),
            canChangeCredentials = !syncManager.isGoogleLogin(),
            canUpgradeAccount = signedInState?.isSignedInAsPlus == true && (isGiftExpiring || FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)),
            canCancelSubscription = signedInState?.isSignedInAsPaid == true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = AccountSectionsState(
            isSubscribedToNewsLetter = false,
            email = null,
            winbackInitParams = WinbackInitParams.Empty,
            canChangeCredentials = false,
            canUpgradeAccount = false,
            canCancelSubscription = false,
        ),
    )

    private suspend fun computeWinbackParams(signInState: SignInState): WinbackInitParams {
        val subscription = (signInState as? SignInState.SignedIn)?.subscription

        return if (subscription?.platform != SubscriptionPlatform.Android) {
            WinbackInitParams.Empty
        } else {
            when (val subscriptionsResult = paymentClient.loadAcknowledgedSubscriptions()) {
                is PaymentResult.Failure -> WinbackInitParams.Empty
                is PaymentResult.Success -> WinbackInitParams(
                    hasGoogleSubscription = subscriptionsResult.value.any { it.isAutoRenewing },
                )
            }
        }
    }

    internal val miniPlayerInset = settings.bottomInset.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0,
    )

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { syncManager.deleteAccountRxSingle().await() }
                val success = response.success ?: false
                deleteAccountState.value = if (success) {
                    DeleteAccountState.Success("OK")
                } else {
                    DeleteAccountState.Failure(response.message)
                }
            } catch (e: Throwable) {
                deleteAccountError(e)
            }
        }
    }

    private fun deleteAccountError(throwable: Throwable) {
        deleteAccountState.value = DeleteAccountState.Failure(message = null)
        Timber.e(throwable)
        crashLogging.sendReport(throwable, message = "Delete account failed")
    }

    fun clearDeleteAccountState() {
        deleteAccountState.value = DeleteAccountState.Empty
    }

    fun updateNewsletter(isChecked: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.NEWSLETTER_OPT_IN_CHANGED,
            mapOf(SOURCE_KEY to NewsletterSource.PROFILE.analyticsValue, ENABLED_KEY to isChecked),
        )
        settings.marketingOptIn.set(isChecked, updateModifiedAt = true)
    }

    internal fun changeSelectedFeatureCard(key: SubscriptionPlan.Key) {
        selectedFeatureCard.value = key
    }

    companion object {
        private const val SOURCE_KEY = "source"
        private const val ENABLED_KEY = "enabled"
    }
}

sealed class DeleteAccountState {
    object Empty : DeleteAccountState()
    data class Success(val result: String) : DeleteAccountState()
    data class Failure(val message: String?) : DeleteAccountState()
}
