package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.time.Instant
import org.junit.Test

class SignInStateTest {

    @Test
    fun isSignedInAsPlusPaid() {
        val email = "support@pocketcasts.com"
        val subscription = Subscription(
            tier = SubscriptionTier.Plus,
            billingCycle = BillingCycle.Monthly,
            platform = SubscriptionPlatform.Android,
            expiryDate = Instant.now(),
            isAutoRenewing = true,
            giftDays = 0,
        )
        // test an Android paying subscriber
        val stateAndroid = SignInState.SignedIn(email = email, subscription = subscription)
        assert(stateAndroid.isSignedInAsPaid)
        // test an iOS paying subscriber
        val stateiOS = SignInState.SignedIn(email = email, subscription = subscription.copy(platform = SubscriptionPlatform.iOS))
        assert(stateiOS.isSignedInAsPaid)
        // test a Web Player paying subscriber
        val stateWebPlayer = SignInState.SignedIn(email = email, subscription = subscription.copy(platform = SubscriptionPlatform.Web))
        assert(stateWebPlayer.isSignedInAsPaid)
        // test a gift user
        val statePayingGift = SignInState.SignedIn(email = email, subscription = subscription.copy(platform = SubscriptionPlatform.Gift))
        assert(!statePayingGift.isSignedInAsPaid)
        // test a paying subscriber
        val statePaying = SignInState.SignedIn(email = email, subscription = subscription)
        assert(statePaying.isSignedInAsPaid)
        // a cancelled subscriber should still have access to the paid features, we don't need to check the expiry as loading the state will covert it to a free account
        val stateCancelled = SignInState.SignedIn(email = email, subscription = subscription.copy(isAutoRenewing = false))
        assert(stateCancelled.isSignedInAsPaid)
        // free users should not have access to paid features
        val stateFree = SignInState.SignedIn(email = email, subscription = null)
        assert(!stateFree.isSignedInAsPaid)
    }
}
