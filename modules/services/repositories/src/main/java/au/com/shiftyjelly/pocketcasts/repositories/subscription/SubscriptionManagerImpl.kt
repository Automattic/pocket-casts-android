package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.toSubscription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager {
    override suspend fun fetchFreshSubscription(): Subscription? {
        return runCatching { syncManager.subscriptionStatus().toSubscription() }
            .onSuccess { subscription ->
                settings.cachedSubscription.set(subscription, updateModifiedAt = false)
                if (subscription != null && !subscription.isChampion && subscription.platform == SubscriptionPlatform.Gift) {
                    settings.setTrialFinishedSeen(false)
                }
            }.getOrNull()
    }

    override fun clearCachedSubscription() {
        settings.cachedSubscription.set(null, updateModifiedAt = false)
    }
}
