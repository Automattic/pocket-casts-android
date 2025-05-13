package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import java.util.Date
import org.junit.Test

class SignInStateTest {

    @Test
    fun isSignedInAsPlusPaid() {
        val email = "support@pocketcasts.com"
        val statusAndroidPaidSubscription = SubscriptionStatus.Paid(
            expiryDate = Date(),
            autoRenew = true,
            giftDays = 0,
            frequency = SubscriptionFrequency.MONTHLY,
            platform = SubscriptionPlatform.ANDROID,
            subscriptions = emptyList(),
            tier = SubscriptionTier.PLUS,
            index = 0,
        )
        // test an Android paying subscriber
        val stateAndroid = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPaidSubscription)
        assert(stateAndroid.isSignedInAsPaid)
        // test an iOS paying subscriber
        val stateiOS = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPaidSubscription.copy(platform = SubscriptionPlatform.IOS))
        assert(stateiOS.isSignedInAsPaid)
        // test a Web Player paying subscriber
        val stateWebPlayer = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPaidSubscription.copy(platform = SubscriptionPlatform.WEB))
        assert(stateWebPlayer.isSignedInAsPaid)
        // test a gift user
        val statePayingGift = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPaidSubscription.copy(platform = SubscriptionPlatform.GIFT))
        assert(!statePayingGift.isSignedInAsPaid)
        // test a paying subscriber
        val statePaying = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPaidSubscription)
        assert(statePaying.isSignedInAsPaid)
        // a cancelled subscriber should still have access to the paid features, we don't need to check the expiry as loading the state will covert it to a free account
        val stateCancelled = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPaidSubscription.copy(autoRenew = false))
        assert(stateCancelled.isSignedInAsPaid)
        // free users should not have access to paid features
        val stateFree = SignInState.SignedIn(email = email, subscriptionStatus = SubscriptionStatus.Free())
        assert(!stateFree.isSignedInAsPaid)
    }
}
