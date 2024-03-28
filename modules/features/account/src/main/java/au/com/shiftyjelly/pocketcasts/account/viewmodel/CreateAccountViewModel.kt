package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CreateAccountViewModel
@Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val podcastManager: PodcastManager,
) : AccountViewModel() {

    val upgradeMode = MutableLiveData<Boolean>()
    val subscriptionType = MutableLiveData<SubscriptionType>().apply { value = SubscriptionType.FREE }
    val subscription = MutableLiveData<Subscription?>()
    val newsletter = MutableLiveData<Boolean>().apply { postValue(false) }
    val termsOfUse = MutableLiveData<Boolean?>()

    val createAccountState = MutableLiveData<CreateAccountState>().apply { value = CreateAccountState.CurrentlyValid }
    private val disposables = CompositeDisposable()
    var defaultSubscriptionType = SubscriptionType.FREE

    @Inject lateinit var subscriptionManager: SubscriptionManager

    companion object {
        private const val PRODUCT_KEY = "product"
        private const val OFFER_TYPE_KEY = "offer_type"
        private const val OFFER_TYPE_NONE = "none"
        private const val OFFER_TYPE_FREE_TRIAL = "free_trial"
        private const val OFFER_TYPE_INTRO_OFFER = "intro_offer"
        private const val ERROR_CODE_KEY = "error_code"
        private const val SOURCE_KEY = "source"
        private const val ENABLED_KEY = "enabled"

        fun trackPurchaseEvent(subscription: Subscription?, purchaseEvent: PurchaseEvent, analyticsTracker: AnalyticsTrackerWrapper) {
            val productKey = subscription?.productDetails?.productId?.let {
                if (it in listOf(PLUS_MONTHLY_PRODUCT_ID, PLUS_YEARLY_PRODUCT_ID)) {
                    // retain short product id for plus subscriptions
                    // extract part of the product id after the last period ("com.pocketcasts.plus.monthly" -> "monthly")
                    it.split('.').lastOrNull()
                } else {
                    it // return full product id for new products
                }
            } ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
            val offerType = when (subscription) {
                is Subscription.Trial -> OFFER_TYPE_FREE_TRIAL
                is Subscription.Intro -> OFFER_TYPE_INTRO_OFFER
                else -> OFFER_TYPE_NONE
            }

            val analyticsProperties = mapOf(
                PRODUCT_KEY to productKey,
                OFFER_TYPE_KEY to offerType,
            )

            when (purchaseEvent) {
                is PurchaseEvent.Success -> analyticsTracker.track(AnalyticsEvent.PURCHASE_SUCCESSFUL, analyticsProperties)

                is PurchaseEvent.Cancelled -> analyticsTracker.track(
                    AnalyticsEvent.PURCHASE_CANCELLED,
                    analyticsProperties.plus(ERROR_CODE_KEY to purchaseEvent.responseCode),
                )

                is PurchaseEvent.Failure -> {
                    // Exclude error_code property if we do not have a responseCode
                    val properties = purchaseEvent.responseCode?.let {
                        analyticsProperties.plus(ERROR_CODE_KEY to it)
                    } ?: analyticsProperties

                    analyticsTracker.track(AnalyticsEvent.PURCHASE_FAILED, properties)
                }
            }
        }
    }

    fun loadSubs() {
        subscriptionManager.observeProductDetails()
            .firstOrError()
            .subscribeBy(
                onSuccess = { productDetailsState ->
                    if (productDetailsState is ProductDetailsState.Loaded) {
                        val subscriptions = productDetailsState.productDetails
                            .mapNotNull {
                                Subscription.fromProductDetails(
                                    productDetails = it,
                                    isOfferEligible = subscriptionManager.isOfferEligible(SubscriptionMapper.mapProductIdToTier(it.productId)),
                                )
                            }
                        val filteredOffer = Subscription.filterOffers(subscriptions)
                        subscriptionManager.getDefaultSubscription(filteredOffer)?.let { updateSubscription(it) }
                        createAccountState.postValue(CreateAccountState.ProductsLoaded(filteredOffer))
                    } else {
                        errorUpdate(CreateAccountError.CANNOT_LOAD_SUBS, true)
                    }
                },
                onError = {
                    errorUpdate(CreateAccountError.CANNOT_LOAD_SUBS, true)
                },
            )
            .addTo(disposables)
    }

    private fun errorUpdate(error: CreateAccountError, add: Boolean) {
        val errors = mutableSetOf<CreateAccountError>()
        when (val existingState = createAccountState.value) {
            is CreateAccountState.Failure -> {
                errors.addAll(existingState.errors)
            }
            else -> {}
        }
        if (add) errors.add(error) else errors.remove(error)
        val newState = if (errors.isEmpty()) {
            CreateAccountState.CurrentlyValid
        } else {
            CreateAccountState.Failure(errors = errors, message = null)
        }
        // update the form state on the main thread if possible, when updating the email and password at the same time the state can be old when using postValue.
        if (Util.isOnMainThread()) {
            createAccountState.value = newState
        } else {
            createAccountState.postValue(newState)
        }
    }

    fun updateSubscriptionType(value: SubscriptionType) {
        subscriptionType.value = value
        createAccountState.postValue(CreateAccountState.SubscriptionTypeChosen)
    }

    fun updateSubscription(value: Subscription) {
        subscription.value = value
    }

    fun updateEmailRefresh() {
        val addError = !isEmailValid(email.value)
        errorUpdate(CreateAccountError.INVALID_EMAIL, addError)
    }

    fun updateEmail(value: String) {
        val valueClean = value.trim()
        email.value = valueClean
        val addError = !isEmailValid(valueClean)
        errorUpdate(CreateAccountError.INVALID_EMAIL, addError)
    }

    fun updatePassword(value: String) {
        password.value = value
        val addError = !isPasswordValid(value)
        errorUpdate(CreateAccountError.INVALID_PASSWORD, addError)
    }

    fun updatePasswordRefresh() {
        val addError = !isPasswordValid(password.value)
        errorUpdate(CreateAccountError.INVALID_PASSWORD, addError)
    }

    fun updateNewsletter(isChecked: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.NEWSLETTER_OPT_IN_CHANGED,
            mapOf(SOURCE_KEY to NewsletterSource.ACCOUNT_UPDATED.analyticsValue, ENABLED_KEY to isChecked),
        )
        newsletter.value = isChecked
        newsletter.value?.let {
            settings.marketingOptIn.set(it, needsSync = true)
        }
    }

    fun updateTermsOfUse(value: Boolean) {
        termsOfUse.value = value
    }

    fun updateStateToFinished() {
        createAccountState.value = CreateAccountState.Finished
    }

    fun clearValues() {
        upgradeMode.value = false
        subscriptionType.value = defaultSubscriptionType
        subscription.value = null
        newsletter.value = false
        termsOfUse.value = null
    }

    fun clearError(error: CreateAccountError) {
        errorUpdate(error, false)
    }

    fun clearReadyForUpgrade() {
        clearValues()
        upgradeMode.value = true
        subscriptionType.value = SubscriptionType.PLUS
    }

    fun currentStateHasError(error: CreateAccountError): Boolean {
        when (val state = createAccountState.value) {
            is CreateAccountState.Failure -> {
                return state.errors.contains(error)
            }
            else -> {}
        }
        return false
    }

    fun sendCreateAccount() {
        val emailString = email.value ?: ""
        val passwordString = password.value ?: ""
        if (emailString.isEmpty() || passwordString.isEmpty()) {
            return
        }

        createAccountState.postValue(CreateAccountState.AccountCreating)

        viewModelScope.launch {
            when (val result = syncManager.createUserWithEmailAndPassword(emailString, passwordString)) {
                is LoginResult.Success -> {
                    analyticsTracker.refreshMetadata()
                    podcastManager.refreshPodcastsAfterSignIn()
                    createAccountState.postValue(CreateAccountState.AccountCreated)
                }
                is LoginResult.Failed -> {
                    val message = result.message
                    val errors = mutableSetOf(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                    createAccountState.postValue(CreateAccountState.Failure(errors, message))
                }
            }
        }
    }

    fun sendCreateSubscriptions() {
        subscriptionManager.observePurchaseEvents()
            .firstOrError()
            .subscribeBy(
                onSuccess = { purchaseEvent ->
                    when (purchaseEvent) {
                        is PurchaseEvent.Success -> {
                            createAccountState.postValue(CreateAccountState.SubscriptionCreated)
                        }
                        is PurchaseEvent.Cancelled -> {
                            errorUpdate(CreateAccountError.CANCELLED_CREATE_SUB, true)
                        }
                        is PurchaseEvent.Failure -> {
                            errorUpdate(CreateAccountError.CANNOT_CREATE_SUB, true)
                        }
                    }
                    trackPurchaseEvent(subscription.value, purchaseEvent, analyticsTracker)
                },
                onError = {
                    errorUpdate(CreateAccountError.CANNOT_CREATE_SUB, true)
                },
            )
            .addTo(disposables)
    }

    fun onCloseDoneForm() {
        analyticsTracker.track(AnalyticsEvent.ACCOUNT_UPDATED_DISMISSED)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

enum class NewsletterSource(val analyticsValue: String) {
    ACCOUNT_UPDATED("account_updated"),
    PROFILE("profile"),
    WELCOME_NEW_ACCOUNT("welcome_new_account"),
}

enum class SubscriptionType(val value: String, val trackingLabel: String) {
    FREE("Free", "free"),
    PLUS("Pocket Casts Plus", "plus"),
}

enum class CreateAccountError {
    CANNOT_LOAD_SUBS,
    INVALID_EMAIL,
    INVALID_PASSWORD,
    CANNOT_CREATE_ACCOUNT,
    CANNOT_CREATE_SUB,
    CANCELLED_CREATE_SUB,
}

sealed class CreateAccountState {
    object CurrentlyValid : CreateAccountState()
    object SubscriptionTypeChosen : CreateAccountState()
    object ProductsLoading : CreateAccountState()
    data class ProductsLoaded(val list: List<Subscription>) : CreateAccountState()
    object AccountCreating : CreateAccountState()
    object AccountCreated : CreateAccountState()
    object SubscriptionCreated : CreateAccountState()
    object Finished : CreateAccountState()
    data class Failure(val errors: MutableSet<CreateAccountError>, val message: String?) : CreateAccountState()
}
