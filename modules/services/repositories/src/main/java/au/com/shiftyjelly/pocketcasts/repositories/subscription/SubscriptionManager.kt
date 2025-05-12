package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

interface SubscriptionManager {
    fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>>

    fun subscriptionTier(): Flow<SubscriptionTier>

    fun getSubscriptionStatusRxSingle(allowCache: Boolean = true): Single<SubscriptionStatus>

    suspend fun getSubscriptionStatus(allowCache: Boolean = true): SubscriptionStatus

    fun getCachedStatus(): SubscriptionStatus?

    fun clearCachedStatus()
}
