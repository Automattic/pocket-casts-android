package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.NewsletterSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import java.time.Duration as JavaDuration

@HiltViewModel
class AccountDetailsViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager,
    userManager: UserManager,
    statsManager: StatsManager,
    private val settings: Settings,
    private val syncManager: SyncManager,
    private val analyticsTracker: AnalyticsTracker,
    private val crashLogging: CrashLogging,
    private val subscriptionMapper: SubscriptionMapper,
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val deleteAccountState = MutableLiveData<DeleteAccountState>().apply { value = DeleteAccountState.Empty }

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
    private val signInState = userManager.getSignInState()
    val signInStateLiveData = signInState.toLiveData()
    val viewState: LiveData<Triple<SignInState, Subscription?, DeleteAccountState>> =
        userManager.getSignInState()
            .combineLatest(subscription)
            .toLiveData()
            .combineLatest(deleteAccountState)
            .map { Triple(it.first.first, it.first.second.get(), it.second) }

    val accountStartDate: LiveData<Date> = MutableLiveData<Date>().apply { value = Date(statsManager.statsStartTime) }

    val marketingOptInState: LiveData<Boolean> =
        settings.marketingOptIn
            .flow
            .asLiveData(viewModelScope.coroutineContext)

    fun deleteAccount() {
        syncManager.deleteAccount()
            .subscribeOn(Schedulers.io())
            .doOnSuccess { response ->
                val success = response.success ?: false
                if (success) {
                    deleteAccountState.postValue(DeleteAccountState.Success("OK"))
                } else {
                    deleteAccountState.postValue(DeleteAccountState.Failure(response.message))
                }
            }
            .subscribeBy(onError = { throwable -> deleteAccountError(throwable) })
            .addTo(disposables)
    }

    private fun deleteAccountError(throwable: Throwable) {
        deleteAccountState.postValue(DeleteAccountState.Failure(message = null))
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
                                        expiresIn = status.expiryDate.toExpiresInDuration(),
                                        frequency = status.frequency,
                                    )
                                } else {
                                    SubscriptionHeaderState.PaidCancel(
                                        tier = status.tier,
                                        expiresIn = status.expiryDate.toExpiresInDuration(),
                                        isChampion = status.isPocketCastsChampion,
                                        platform = status.platform,
                                        giftDaysLeft = status.giftDays,
                                    )
                                }
                            } else if (activeSubscription.autoRenewing) {
                                SubscriptionHeaderState.SupporterRenew(
                                    tier = activeSubscription.tier,
                                    expiresIn = activeSubscription.expiryDate?.toExpiresInDuration(),
                                )
                            } else {
                                SubscriptionHeaderState.SupporterCancel(
                                    tier = activeSubscription.tier,
                                    expiresIn = activeSubscription.expiryDate?.toExpiresInDuration(),
                                )
                            }
                        }
                    },
                )
            }
        }
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = AccountHeaderState.empty())

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    private fun Date.toExpiresInDuration(): Duration {
        return JavaDuration.between(Instant.now(), toInstant())
            .toKotlinDuration()
            .coerceAtLeast(Duration.ZERO)
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
