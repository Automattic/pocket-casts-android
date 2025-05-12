package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.toStatus
import au.com.shiftyjelly.pocketcasts.utils.Optional
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager {
    private var cachedSubscriptionStatus: SubscriptionStatus?
        get() = settings.cachedSubscriptionStatus.value
        set(value) = settings.cachedSubscriptionStatus.set(value, updateModifiedAt = false)

    private val subscriptionChangedEvents = PublishRelay.create<SubscriptionChangedEvent>()

    override fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>> {
        return settings.cachedSubscriptionStatus.flow
            .map { status -> Optional.of(status) }
            .asFlowable()
    }

    override fun subscriptionTier(): Flow<SubscriptionTier> {
        return observeSubscriptionStatus().asFlow().map { status ->
            (status.get() as? SubscriptionStatus.Paid)?.tier ?: SubscriptionTier.NONE
        }.distinctUntilChanged()
    }

    override fun getSubscriptionStatusRxSingle(allowCache: Boolean): Single<SubscriptionStatus> {
        return rxSingle {
            getSubscriptionStatus(allowCache)
        }
    }

    override suspend fun getSubscriptionStatus(allowCache: Boolean): SubscriptionStatus {
        val cache = cachedSubscriptionStatus
        if (cache != null && allowCache) {
            return cache
        }

        val status = syncManager.subscriptionStatus().toStatus()

        val oldStatus = cachedSubscriptionStatus
        if (oldStatus != status) {
            if (status is SubscriptionStatus.Paid && oldStatus is SubscriptionStatus.Free) {
                subscriptionChangedEvents.accept(SubscriptionChangedEvent.AccountUpgradedToPlus)
            } else if (status is SubscriptionStatus.Free && oldStatus is SubscriptionStatus.Paid) {
                subscriptionChangedEvents.accept(SubscriptionChangedEvent.AccountDowngradedToFree)
            }
        }
        cachedSubscriptionStatus = status

        if (!status.isPocketCastsChampion && status is SubscriptionStatus.Paid && status.platform == SubscriptionPlatform.GIFT) { // This account is a trial account
            settings.setTrialFinishedSeen(false) // Make sure on expiry we show the trial finished dialog
        }
        return status
    }

    override fun getCachedStatus(): SubscriptionStatus? {
        return cachedSubscriptionStatus
    }

    override fun clearCachedStatus() {
        cachedSubscriptionStatus = null
    }
}

sealed class SubscriptionChangedEvent {
    object AccountUpgradedToPlus : SubscriptionChangedEvent()
    object AccountDowngradedToFree : SubscriptionChangedEvent()
}
