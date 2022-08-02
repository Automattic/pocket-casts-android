package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.util.BillingPeriodHelper
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringBillingPeriod
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel
@Inject constructor(
    private val auth: AccountAuth,
    private val settings: Settings,
    val billingPeriodHelper: BillingPeriodHelper,
) : AccountViewModel() {

    val upgradeMode = MutableLiveData<Boolean>()
    val subscriptionType = MutableLiveData<SubscriptionType>().apply { value = SubscriptionType.FREE }
    val subscriptionFrequency = MutableLiveData<SubscriptionFrequency?>()
    val newsletter = MutableLiveData<Boolean>().apply { postValue(false) }
    val termsOfUse = MutableLiveData<Boolean?>()

    val createAccountState = MutableLiveData<CreateAccountState>().apply { value = CreateAccountState.CurrentlyValid }
    private val disposables = CompositeDisposable()
    var defaultSubscriptionType = SubscriptionType.FREE
    var supporterInstance = false

    @Inject lateinit var subscriptionManager: SubscriptionManager

    fun loadSubs() {
        subscriptionManager.observeProductDetails()
            .firstOrError()
            .subscribeBy(
                onSuccess = { productDetailsState ->
                    if (productDetailsState is ProductDetailsState.Loaded) {

                        val list = mutableListOf<SubscriptionFrequency>()

                        productDetailsState.productDetails.forEach { productDetails ->
                            val billingPeriod = productDetails.recurringBillingPeriod
                            val billingDetails = billingPeriod?.let { billingPeriodHelper.mapToBillingDetails(it) }

                            // FIXME Include trial information for displaying in the UI

                            val subscriptionFrequency = SubscriptionFrequency(
                                product = productDetails,
                                period = billingDetails?.periodUnit,
                                renews = billingDetails?.renews,
                                hint = billingDetails?.hint,
                                isMonth = billingDetails?.isMonth ?: true
                            )
                            list.add(subscriptionFrequency)
                        }
                        if (list.isNotEmpty()) {
                            updateSubscriptionFrequency(list.last())
                        }
                        createAccountState.postValue(CreateAccountState.ProductsLoaded(list))
                    } else {
                        errorUpdate(CreateAccountError.CANNOT_LOAD_SUBS, true)
                    }
                },
                onError = {
                    errorUpdate(CreateAccountError.CANNOT_LOAD_SUBS, true)
                }
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

    fun updateSubscriptionFrequency(value: SubscriptionFrequency) {
        subscriptionFrequency.value = value
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

    fun updateNewsletter(value: Boolean) {
        newsletter.value = value
        newsletter.value?.let {
            settings.setMarketingOptIn(it)
            settings.setMarketingOptInNeedsSync(true)
        }
    }

    fun updateTermsOfUse(value: Boolean) {
        termsOfUse.value = value
    }

    fun updateStateTotAccountCreated() {
        createAccountState.value = CreateAccountState.AccountCreated
    }

    fun updateStateToFinished() {
        createAccountState.value = CreateAccountState.Finished
    }

    fun clearValues() {
        upgradeMode.value = false
        subscriptionType.value = defaultSubscriptionType
        subscriptionFrequency.value = null
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
            when (val result = auth.createUserWithEmailAndPassword(emailString, passwordString)) {
                is AccountAuth.AuthResult.Success -> {
                    createAccountState.postValue(CreateAccountState.AccountCreated)
                }
                is AccountAuth.AuthResult.Failed -> {
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
                        else -> {
                            errorUpdate(CreateAccountError.CANNOT_CREATE_SUB, true)
                        }
                    }
                },
                onError = {
                    errorUpdate(CreateAccountError.CANNOT_CREATE_SUB, true)
                }
            )
            .addTo(disposables)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

enum class SubscriptionType(val value: String) {
    FREE("Free"),
    PLUS("Pocket Casts Plus")
}

data class SubscriptionFrequency(
    val product: ProductDetails,
    @StringRes val period: Int?,
    @StringRes val renews: Int?,
    @StringRes val hint: Int?,
    val isMonth: Boolean
)

enum class CreateAccountError {
    CANNOT_LOAD_SUBS,
    INVALID_EMAIL,
    INVALID_PASSWORD,
    CANNOT_CREATE_ACCOUNT,
    CANNOT_CREATE_SUB,
    CANCELLED_CREATE_SUB
}

sealed class CreateAccountState {
    object CurrentlyValid : CreateAccountState()
    object SubscriptionTypeChosen : CreateAccountState()
    object ProductsLoading : CreateAccountState()
    data class ProductsLoaded(val list: List<SubscriptionFrequency>) : CreateAccountState()
    object AccountCreating : CreateAccountState()
    object AccountCreated : CreateAccountState()
    object SubscriptionCreating : CreateAccountState()
    object SubscriptionCreated : CreateAccountState()
    object Finished : CreateAccountState()
    data class Failure(val errors: MutableSet<CreateAccountError>, val message: String?) : CreateAccountState()
}
