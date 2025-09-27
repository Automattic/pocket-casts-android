package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.type.Subscription

interface SubscriptionManager {
    suspend fun fetchFreshSubscription(): Subscription?

    fun clearCachedMembership()
}
