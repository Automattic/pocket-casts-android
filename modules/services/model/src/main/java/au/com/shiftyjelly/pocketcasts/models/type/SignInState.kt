package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.time.Instant

sealed interface SignInState {
    val isSignedIn: Boolean

    val isSignedInAsFree: Boolean

    val isSignedOut: Boolean
        get() = !isSignedIn

    val isNoAccountOrFree: Boolean
        get() = !isSignedIn || isSignedInAsFree

    val isSignedInAsPlus: Boolean

    val isSignedInAsPatron: Boolean

    val isSignedInAsPlusOrPatron: Boolean
        get() = isSignedInAsPlus || isSignedInAsPatron

    val isSignedInAsPaid: Boolean

    val isExpiredTrial: Boolean

    data class SignedIn(
        val email: String,
        val subscription: Subscription?,
    ) : SignInState {
        override val isSignedIn
            get() = true

        override val isSignedInAsFree
            get() = subscription == null

        override val isSignedInAsPlus
            get() = subscription?.tier == SubscriptionTier.Plus

        override val isSignedInAsPatron: Boolean
            get() = subscription?.tier == SubscriptionTier.Patron

        override val isSignedInAsPaid: Boolean
            get() = subscription?.platform?.isPaid == true

        override val isExpiredTrial: Boolean
            get() = subscription?.platform == SubscriptionPlatform.Gift && subscription.expiryDate < Instant.now()
    }

    data object SignedOut : SignInState {
        override val isSignedIn
            get() = false

        override val isSignedInAsFree
            get() = false

        override val isSignedInAsPlus
            get() = false

        override val isSignedInAsPatron: Boolean
            get() = false

        override val isSignedInAsPaid: Boolean
            get() = false

        override val isExpiredTrial: Boolean
            get() = false
    }
}
