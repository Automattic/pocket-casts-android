package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.account.viewmodel.NewsletterSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AccountDetailsViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager,
    userManager: UserManager,
    statsManager: StatsManager,
    private val settings: Settings,
    private val syncServerManager: SyncServerManager,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val deleteAccountState = MutableLiveData<DeleteAccountState>().apply { value = DeleteAccountState.Empty }

    private val subscription = subscriptionManager.observeProductDetails().map { state ->
        if (state is ProductDetailsState.Loaded) {
            val subscriptions = state.productDetails
                .mapNotNull {
                    Subscription.fromProductDetails(
                        productDetails = it,
                        isFreeTrialEligible = subscriptionManager.isFreeTrialEligible()
                    )
                }
            Optional.of(subscriptionManager.getDefaultSubscription(subscriptions))
        } else {
            Optional.empty()
        }
    }
    val signInState = userManager.getSignInState().toLiveData()
    val viewState: LiveData<Triple<SignInState, Subscription?, DeleteAccountState>> =
        userManager.getSignInState()
            .combineLatest(subscription)
            .toLiveData()
            .combineLatest(deleteAccountState)
            .map { Triple(it.first.first, it.first.second.get(), it.second) }

    val accountStartDate: LiveData<Date> = MutableLiveData<Date>().apply { value = Date(statsManager.statsStartTime) }

    val marketingOptInState: LiveData<Boolean> =
        settings.marketingOptObservable
            .distinctUntilChanged()
            .toFlowable(BackpressureStrategy.LATEST)
            .toLiveData()

    fun deleteAccount() {
        syncServerManager.deleteAccount()
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
        SentryHelper.recordException("Delete account failed", throwable)
    }

    fun clearDeleteAccountState() {
        deleteAccountState.value = DeleteAccountState.Empty
    }

    fun updateNewsletter(isChecked: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.NEWSLETTER_OPT_IN_CHANGED,
            mapOf(SOURCE_KEY to NewsletterSource.PROFILE.analyticsValue, ENABLED_KEY to isChecked)
        )
        settings.setMarketingOptIn(isChecked)
        settings.setMarketingOptInNeedsSync(true)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
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
