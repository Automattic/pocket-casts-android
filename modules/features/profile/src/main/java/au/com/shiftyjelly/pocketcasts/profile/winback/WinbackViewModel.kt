package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
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
    ) { signInState, productDetails ->
        UiState(
            subscriptionsState = createAvailablePlans(signInState, productDetails),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    internal data class UiState(
        val subscriptionsState: SubscriptionsState,
    ) {
        companion object {
            val Empty = UiState(
                subscriptionsState = SubscriptionsState.Loading,
            )
        }
    }
}

internal sealed interface SubscriptionsState {
    data object Loading : SubscriptionsState

    data object Failure : SubscriptionsState

    data class Loaded(
        val userSubscription: Subscription.Simple?,
        val subscriptions: List<Subscription.Simple>,
    ) : SubscriptionsState
}

private fun createAvailablePlans(
    signInState: SignInState,
    productDetailsState: ProductDetailsState,
): SubscriptionsState {
    val primarySubscription = signInState.findPrimarySubscription()
    if (primarySubscription == null) {
        return SubscriptionsState.Failure
    }

    return when (productDetailsState) {
        is ProductDetailsState.Loading -> SubscriptionsState.Loading
        is ProductDetailsState.Failure -> SubscriptionsState.Failure
        is ProductDetailsState.Loaded -> {
            val mapper = SubscriptionMapper()
            val subscriptions = productDetailsState.productDetails
                .map(mapper::mapFromProductDetails)
                .filterIsInstance<Subscription.Simple>()
                .sortedWith(SubscriptionsComparator)
            val matchingSubscription = subscriptions.find { it.productDetails.productId == primarySubscription.plan }
            SubscriptionsState.Loaded(matchingSubscription, subscriptions)
        }
    }
}

private fun SignInState.findPrimarySubscription() = when (this) {
    is SignInState.SignedOut -> null
    is SignInState.SignedIn -> when (val subscriptionStatus = subscriptionStatus) {
        is SubscriptionStatus.Free -> null
        is SubscriptionStatus.Paid -> subscriptionStatus.subscriptions.find { it.isPrimarySubscription }
    }
}

private object SubscriptionsComparator : Comparator<Subscription.Simple> {
    private val priorities = mapOf(
        Subscription.PLUS_MONTHLY_PRODUCT_ID to 0,
        Subscription.PATRON_MONTHLY_PRODUCT_ID to 1,
        Subscription.PLUS_YEARLY_PRODUCT_ID to 2,
        Subscription.PATRON_YEARLY_PRODUCT_ID to 3,
    )

    override fun compare(o1: Subscription.Simple, o2: Subscription.Simple): Int {
        val priority1 = priorities[o1.productDetails.productId]
        val priority2 = priorities[o2.productDetails.productId]
        return when {
            priority1 != null && priority2 != null -> priority1 - priority2
            priority1 != null && priority2 == null -> -1
            priority1 == null && priority2 != null -> 1
            else -> o1.productDetails.title.compareTo(o2.productDetails.title)
        }
    }
}
