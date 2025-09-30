package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.toMembership
import au.com.shiftyjelly.pocketcasts.utils.coroutines.SyncedAction
import au.com.shiftyjelly.pocketcasts.utils.coroutines.run
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager {
    private val fetchMembershipAction = SyncedAction<Unit, Membership?> {
        runCatching { syncManager.subscriptionStatus().toMembership() }
            .onSuccess { membership ->
                val subscription = membership.subscription
                settings.cachedMembership.set(membership, updateModifiedAt = false)
                if (subscription != null && !subscription.isChampion && subscription.platform == SubscriptionPlatform.Gift) {
                    settings.setTrialFinishedSeen(false)
                }
            }
            .getOrNull()
    }

    override suspend fun fetchFreshSubscription(): Subscription? {
        return fetchMembershipAction.run(scope)?.subscription
    }

    override fun clearCachedMembership() {
        settings.cachedMembership.set(Membership.Empty, updateModifiedAt = false)
    }
}
