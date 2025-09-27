package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.toMembership
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager {
    override suspend fun fetchFreshSubscription(): Subscription? {
        return runCatching { syncManager.subscriptionStatus().toMembership() }
            .onSuccess { membership ->
                val subscription = membership.subscription
                settings.cachedMembership.set(membership, updateModifiedAt = false)
                if (subscription != null && !subscription.isChampion && subscription.platform == SubscriptionPlatform.Gift) {
                    settings.setTrialFinishedSeen(false)
                }
            }
            .map { membership -> membership.subscription }
            .getOrNull()
    }

    override fun clearCachedMembership() {
        settings.cachedMembership.set(Membership.Empty, updateModifiedAt = false)
    }
}
