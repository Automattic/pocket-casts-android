package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.NewsletterSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.winback.WinbackInitParams
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.Optional
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
    private val subscriptionManager: SubscriptionManager,
    private val syncManager: SyncManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val crashLogging: CrashLogging,
    private val subscriptionMapper: SubscriptionMapper,
) : ViewModel() {
    internal val deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Empty)
    internal val signInState = userManager.getSignInState()
    internal val marketingOptIn = settings.marketingOptIn.flow
    private val subscription = subscriptionManager.observeProductDetails().map { state ->
        if (state is ProductDetailsState.Loaded) {
            val subscriptions = state.productDetails
                .mapNotNull {
                    subscriptionMapper.mapFromProductDetails(
                        productDetails = it,
                        isOfferEligible = subscriptionManager.isOfferEligible(
                            SubscriptionTier.fromProductId(it.productId),
                        ),
                    )
                }
            val filteredOffer = Subscription.filterOffers(subscriptions)
            Optional.of(subscriptionManager.getDefaultSubscription(filteredOffer))
        } else {
            Optional.empty()
        }
    }

    internal val headerState = signInState.asFlow().map { state ->
        when (state) {
            is SignInState.SignedOut -> AccountHeaderState.empty()
            is SignInState.SignedIn -> {
                val status = state.subscriptionStatus
                AccountHeaderState(
                    email = state.email,
                    imageUrl = Gravatar.getUrl(state.email),
                    subscription = when (status) {
                        is SubscriptionStatus.Free -> SubscriptionHeaderState.Free
                        is SubscriptionStatus.Paid -> {
                            val activeSubscription = status.subscriptions.getOrNull(status.index)
                            if (activeSubscription == null || activeSubscription.tier in paidTiers) {
                                if (status.autoRenew) {
                                    SubscriptionHeaderState.PaidRenew(
                                        tier = status.tier,
                                        expiresIn = status.expiryDate.toDurationFromNow(),
                                        frequency = status.frequency,
                                    )
                                } else {
                                    SubscriptionHeaderState.PaidCancel(
                                        tier = status.tier,
                                        expiresIn = status.expiryDate.toDurationFromNow(),
                                        isChampion = status.isPocketCastsChampion,
                                        platform = status.platform,
                                        giftDaysLeft = status.giftDays,
                                    )
                                }
                            } else if (activeSubscription.autoRenewing) {
                                SubscriptionHeaderState.SupporterRenew(
                                    tier = activeSubscription.tier,
                                    expiresIn = activeSubscription.expiryDate?.toDurationFromNow(),
                                    isChampion = status.isPocketCastsChampion,
                                )
                            } else {
                                SubscriptionHeaderState.SupporterCancel(
                                    tier = activeSubscription.tier,
                                    expiresIn = activeSubscription.expiryDate?.toDurationFromNow(),
                                    isChampion = status.isPocketCastsChampion,
                                )
                            }
                        }
                    },
                )
            }
        }
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = AccountHeaderState.empty())

    internal val showUpgradeBanner = combine(
        signInState.asFlow(),
        subscription.asFlow(),
    ) { signInState, subscription ->
        val signedInState = signInState as? SignInState.SignedIn
        val isGiftExpiring = (signedInState?.subscriptionStatus as? SubscriptionStatus.Paid)?.isExpiring == true
        subscription != null && (signInState.isSignedInAsFree || isGiftExpiring)
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = false)

    internal val sectionsState = combine(
        signInState.asFlow(),
        marketingOptIn,
    ) { signInState, marketingOptIn ->
        val signedInState = signInState as? SignInState.SignedIn
        val isGiftExpiring = (signedInState?.subscriptionStatus as? SubscriptionStatus.Paid)?.isExpiring == true
        AccountSectionsState(
            isSubscribedToNewsLetter = marketingOptIn,
            email = signedInState?.email,
            winbackInitParams = computeWinbackParams(signInState),
            canChangeCredentials = !syncManager.isGoogleLogin(),
            canUpgradeAccount = signedInState?.isSignedInAsPlus == true && isGiftExpiring,
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
        val paidSubscriptionStatus = (signInState as? SignInState.SignedIn)?.subscriptionStatus as? SubscriptionStatus.Paid
        val subscriptionPlatform = paidSubscriptionStatus?.platform

        return if (subscriptionPlatform != SubscriptionPlatform.ANDROID) {
            WinbackInitParams.Empty
        } else {
            when (val purchasesState = subscriptionManager.loadPurchases()) {
                is PurchasesState.Failure -> WinbackInitParams.Empty
                is PurchasesState.Loaded -> WinbackInitParams(
                    hasGoogleSubscription = purchasesState.purchases
                        .filter { it.isAcknowledged && it.isAutoRenewing }
                        .isNotEmpty(),
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

    companion object {
        private const val SOURCE_KEY = "source"
        private const val ENABLED_KEY = "enabled"

        private val paidTiers = listOf(SubscriptionTier.PLUS, SubscriptionTier.PATRON)
    }
}

sealed class DeleteAccountState {
    object Empty : DeleteAccountState()
    data class Success(val result: String) : DeleteAccountState()
    data class Failure(val message: String?) : DeleteAccountState()
}
