package au.com.shiftyjelly.pocketcasts.repositories.subscription

import android.app.Activity
import android.content.Context
import androidx.lifecycle.AtomicReference
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ClientConnection.ClientConnectionState.Connected
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ClientConnection.ClientConnectionState.Connecting
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ClientConnection.ClientConnectionState.Disconnected
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ClientConnection.ClientConnectionState.Uninitialized
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext context: Context,
    private val productDetailsInterceptor: ProductDetailsInterceptor,
) {
    private val _purchaseUpdates = MutableSharedFlow<Pair<BillingResult, List<Purchase>>>(
        extraBufferCapacity = 100, // Arbitrarily large number
    )
    val purchaseUpdates = _purchaseUpdates.asSharedFlow()

    private val connection = ClientConnection(
        context,
        listener = PurchasesUpdatedListener { billingResult, purchases ->
            logSubscriptionInfo("Purchase results updated")
            _purchaseUpdates.tryEmit(billingResult to purchases.orEmpty())
        },
    )

    suspend fun loadProducts(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>> {
        logSubscriptionInfo("Loading products")
        return connection.withConnectedClient { client ->
            val productDetailsResult = client.queryProductDetails(params)
            val result = productDetailsInterceptor.intercept(
                productDetailsResult.billingResult,
                productDetailsResult.productDetailsList.orEmpty(),
            )
            if (result.first.isOk()) {
                logSubscriptionInfo("Products loaded")
            } else {
                logSubscriptionWarning("Failed to load products: ${result.first.debugMessage}")
            }
            result
        }
    }

    suspend fun loadPurchaseHistory(
        params: QueryPurchaseHistoryParams,
    ): Pair<BillingResult, List<PurchaseHistoryRecord>> {
        logSubscriptionInfo("Loading purchase history")
        return connection.withConnectedClient { client ->
            val result = client.queryPurchaseHistory(params)
            if (result.billingResult.isOk()) {
                logSubscriptionInfo("Purchase history loaded")
            } else {
                logSubscriptionWarning("Failed to load purchase history: ${result.billingResult.debugMessage}")
            }
            result.billingResult to result.purchaseHistoryRecordList.orEmpty()
        }
    }

    suspend fun loadPurchases(
        params: QueryPurchasesParams,
    ): Pair<BillingResult, List<Purchase>> {
        logSubscriptionInfo("Loading purchases")
        return connection.withConnectedClient { client ->
            val result = client.queryPurchasesAsync(params)
            if (result.billingResult.isOk()) {
                logSubscriptionInfo("Purchases loaded")
            } else {
                logSubscriptionWarning("Failed to load purchases: ${result.billingResult.debugMessage}")
            }
            result.billingResult to result.purchasesList
        }
    }

    suspend fun acknowledgePurchase(
        params: AcknowledgePurchaseParams,
    ): BillingResult {
        logSubscriptionInfo("Acknowledging purchase: ${params.purchaseToken}")
        return connection.withConnectedClient { client ->
            val result = client.acknowledgePurchase(params)
            if (result.isOk()) {
                logSubscriptionInfo("Purchase acknowledge: ${params.purchaseToken}")
            } else {
                logSubscriptionWarning("Failed to acknowledge purchase: ${params.purchaseToken}, ${result.debugMessage}")
            }
            result
        }
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult {
        logSubscriptionInfo("Launching billing flow")
        return connection.withConnectedClient { client ->
            val result = client.launchBillingFlow(activity, params)
            if (result.isOk()) {
                logSubscriptionInfo("Launched billing flow")
            } else {
                logSubscriptionWarning("Failed to launch billing flow: ${result.debugMessage}")
            }
            result
        }
    }
}

private class ClientConnection(
    context: Context,
    listener: PurchasesUpdatedListener,
) {
    private val connectionState = AtomicReference<ClientConnectionState>(Uninitialized)
    private val connectionMutex = Mutex()

    private val billingClient = run {
        val params = PendingPurchasesParams.newBuilder()
            .enablePrepaidPlans()
            .enableOneTimeProducts()
            .build()
        BillingClient.newBuilder(context)
            .enablePendingPurchases(params)
            .setListener(listener)
            .build()
    }

    suspend fun <T> withConnectedClient(block: suspend (BillingClient) -> T): T {
        connect()
        return block(billingClient)
    }

    private suspend fun connect() = connectionMutex.withLock {
        val state = connectionState.getAndUpdate { currentState ->
            if (currentState != Connected) Connecting else currentState
        }
        logSubscriptionInfo("Billing client connection: $state")
        if (state == Disconnected || state == Uninitialized) {
            val isConnectionEstablished = setupBillingClient()
            if (!isConnectionEstablished) {
                billingClient.endConnection()
            }
            connectionState.updateAndGet { currentState ->
                if (isConnectionEstablished && currentState == Connecting) Connected else Disconnected
            }
        }
    }

    private suspend fun setupBillingClient(): Boolean {
        logSubscriptionInfo("Connecting to billing client")
        return suspendCancellableCoroutine<Boolean> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    logSubscriptionInfo("Billing setup finished: $billingResult")
                    continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    logSubscriptionWarning("Billing client disconnected")

                    // Emitting disconnected state here as well as this an ongoing listener
                    // And we  want to update the status if this changes
                    billingClient.endConnection()
                    connectionState.set(Disconnected)

                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            })
        }
    }

    private enum class ClientConnectionState {
        Uninitialized,
        Disconnected,
        Connecting,
        Connected,
    }
}

internal fun BillingResult.isOk() = responseCode == BillingClient.BillingResponseCode.OK
