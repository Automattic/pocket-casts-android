package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.toMembership
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager {
    private val fetchMutex = Mutex()
    private var fetchCallsCount = 0
    private var inFlightSubscription: Deferred<Subscription?>? = null

    override suspend fun fetchFreshSubscription(): Subscription? {
        val deferred = fetchMutex.withLock {
            val inFlight = inFlightSubscription ?: scope.async(start = CoroutineStart.LAZY) { fetchSubscription() }
            inFlightSubscription = inFlight
            fetchCallsCount++
            inFlight
        }

        return try {
            deferred.await()
        } finally {
            fetchMutex.withLock {
                fetchCallsCount--
                if (fetchCallsCount == 0) {
                    inFlightSubscription = null
                }
            }
        }
    }

    private suspend fun fetchSubscription(): Subscription? {
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
