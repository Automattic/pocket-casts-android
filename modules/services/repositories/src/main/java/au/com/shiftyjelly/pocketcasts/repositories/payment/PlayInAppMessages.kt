package au.com.shiftyjelly.pocketcasts.repositories.payment

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.InAppMessageResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ProcessLifecycle
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class PlayInAppMessages @Inject constructor(
    private val paymentClient: PaymentClient,
    private val subscriptionManager: SubscriptionManager,
    private val syncManager: SyncManager,
    private val settings: Settings,
    @ApplicationScope private val scope: CoroutineScope,
    @ProcessLifecycle processLifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver {
    private var isShownInForeground = false
    private var pendingMessages: Job? = null

    init {
        processLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        isShownInForeground = false
    }

    fun show(activity: Activity) {
        if (isShownInForeground || pendingMessages?.isActive == true || !isEligible()) {
            return
        }
        isShownInForeground = true
        pendingMessages = scope.launch(Dispatchers.Main) {
            val result = withTimeoutOrNull(PendingMessagesTimeout) { paymentClient.showInAppMessages(activity) }
            if (result == null) {
                LogBuffer.i(LogBuffer.TAG_SUBSCRIPTIONS, "In-app messages timed out")
            }
            if (result is PaymentResult.Success && result.value is InAppMessageResult.SubscriptionStatusUpdated) {
                refreshSubscription()
            }
        }
    }

    private fun isEligible(): Boolean {
        val platform = settings.cachedMembership.value.subscription?.platform
        return FeatureFlag.isEnabled(Feature.PLAY_IN_APP_MESSAGES) &&
            syncManager.isLoggedIn() &&
            platform !in NonPlayPlatforms
    }

    private suspend fun refreshSubscription() {
        // The server learns about the Play change asynchronously, so the first fetch can still be stale
        subscriptionManager.fetchFreshSubscription()
        delay(RefreshRetryDelay)
        subscriptionManager.fetchFreshSubscription()
    }

    private companion object {
        val NonPlayPlatforms = setOf(SubscriptionPlatform.iOS, SubscriptionPlatform.Web, SubscriptionPlatform.Gift)
        val RefreshRetryDelay = 5.seconds
        val PendingMessagesTimeout = 30.minutes
    }
}
