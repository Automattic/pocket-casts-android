package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import org.junit.Test
import java.util.Date

class SignInStateTest {

    @Test
    fun isSignedInAsPlusPaid() {
        val email = "support@pocketcasts.com"
        val statusAndroidPlusPaid = SubscriptionStatus.Plus(
            expiry = Date(),
            autoRenew = true,
            giftDays = 0,
            frequency = SubscriptionFrequency.MONTHLY,
            platform = SubscriptionPlatform.ANDROID,
            subscriptionList = emptyList(),
            type = SubscriptionType.PLUS,
            index = 0
        )
        // test an Android paying Plus subscriber
        val stateAndroid = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPlusPaid)
        assert(stateAndroid.isSignedInAsPlusPaid)
        // test an iOS paying Plus subscriber
        val stateiOS = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPlusPaid.copy(platform = SubscriptionPlatform.IOS))
        assert(stateiOS.isSignedInAsPlusPaid)
        // test a Web Player paying Plus subscriber
        val stateWebPlayer = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPlusPaid.copy(platform = SubscriptionPlatform.WEB))
        assert(stateWebPlayer.isSignedInAsPlusPaid)
        // test a gift Plus user
        val statePayingGift = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPlusPaid.copy(platform = SubscriptionPlatform.GIFT))
        assert(!statePayingGift.isSignedInAsPlusPaid)
        // test a paying Plus subscriber
        val statePaying = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPlusPaid)
        assert(statePaying.isSignedInAsPlusPaid)
        // a cancelled Plus subscriber should still have access to the paid Plus features, we don't need to check the expiry as loading the state will covert it to a free account
        val stateCancelled = SignInState.SignedIn(email = email, subscriptionStatus = statusAndroidPlusPaid.copy(autoRenew = false))
        assert(stateCancelled.isSignedInAsPlusPaid)
        // free users should not have access to paid Plus features
        val stateFree = SignInState.SignedIn(email = email, subscriptionStatus = SubscriptionStatus.Free())
        assert(!stateFree.isSignedInAsPlusPaid)
    }
}
