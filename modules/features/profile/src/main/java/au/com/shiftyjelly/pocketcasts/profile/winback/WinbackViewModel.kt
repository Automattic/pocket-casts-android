package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class WinbackViewModel @Inject constructor(
    val userManager: UserManager,
    val subscriptionManager: SubscriptionManager,
) : ViewModel() {
    private val signInState = userManager.getSignInState().asFlow()

    private val productDetails = subscriptionManager.observeProductDetails().asFlow()

    internal val uiState = combine(
        signInState,
        productDetails,
    ) { signInState, productsState ->
        val primarySubscription = signInState.findPrimarySubscription()
        UiState(
            userSubscriptionId = primarySubscription?.plan,
            googleBillingProducts = when (productsState) {
                is ProductDetailsState.Failure, ProductDetailsState.Loading -> emptyList()
                is ProductDetailsState.Loaded -> productsState.productDetails
            },
            availablePlans = if (primarySubscription != null) {
                createAvailablePlans(productsState)
            } else {
                AvailablePlans.Failure
            },

        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    internal data class UiState(
        val userSubscriptionId: String?,
        val googleBillingProducts: List<ProductDetails>,
        val availablePlans: AvailablePlans,
    ) {
        companion object {
            val Empty = UiState(
                userSubscriptionId = null,
                googleBillingProducts = emptyList(),
                availablePlans = AvailablePlans.Loading,
            )
        }
    }
}

internal data class SubscriptionPlan(
    val productId: String,
    val offerToken: String,
    val name: String,
    val formattedPrice: String,
    val billingPeriod: BillingPeriod,
)

internal enum class BillingPeriod {
    Monthly,
    Yearly,
}

internal sealed interface AvailablePlans {
    data object Loading : AvailablePlans

    data object Failure : AvailablePlans

    data class Loaded(
        val plans: List<SubscriptionPlan>,
    ) : AvailablePlans
}

private fun SignInState.findPrimarySubscription() = when (this) {
    is SignInState.SignedOut -> null
    is SignInState.SignedIn -> when (val subscriptionStatus = subscriptionStatus) {
        is SubscriptionStatus.Free -> null
        is SubscriptionStatus.Paid -> subscriptionStatus.subscriptions.find { it.isPrimarySubscription }
    }
}

private fun createAvailablePlans(
    productDetailsState: ProductDetailsState,
) = when (productDetailsState) {
    is ProductDetailsState.Loading -> AvailablePlans.Loading
    is ProductDetailsState.Failure -> AvailablePlans.Failure
    is ProductDetailsState.Loaded -> {
        val mapper = SubscriptionMapper()
        val plans = productDetailsState.productDetails
            .map(mapper::mapFromProductDetails)
            .filterIsInstance<Subscription.Simple>()
            .map(Subscription.Simple::toPlan)
            .sortedWith(PlanComparator)
        AvailablePlans.Loaded(plans)
    }
}

private fun Subscription.Simple.toPlan() = SubscriptionPlan(
    productId = productDetails.productId,
    offerToken = offerToken,
    name = shortTitle,
    formattedPrice = recurringPricingPhase.formattedPrice,
    billingPeriod = when (recurringPricingPhase) {
        is SubscriptionPricingPhase.Months -> BillingPeriod.Monthly
        is SubscriptionPricingPhase.Years -> BillingPeriod.Yearly
    },
)

private object PlanComparator : Comparator<SubscriptionPlan> {
    private val priorities = mapOf(
        Subscription.PLUS_MONTHLY_PRODUCT_ID to 0,
        Subscription.PATRON_MONTHLY_PRODUCT_ID to 1,
        Subscription.PLUS_YEARLY_PRODUCT_ID to 2,
        Subscription.PATRON_YEARLY_PRODUCT_ID to 3,
    )

    override fun compare(o1: SubscriptionPlan, o2: SubscriptionPlan): Int {
        val priority1 = priorities[o1.productId]
        val priority2 = priorities[o2.productId]
        return when {
            priority1 != null && priority2 != null -> priority1 - priority2
            priority1 != null && priority2 == null -> -1
            priority1 == null && priority2 != null -> 1
            else -> o1.name.compareTo(o2.name)
        }
    }
}
