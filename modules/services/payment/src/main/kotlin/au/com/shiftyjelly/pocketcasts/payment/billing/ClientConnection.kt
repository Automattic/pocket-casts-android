package au.com.shiftyjelly.pocketcasts.payment.billing

import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.Logger
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ClientConnection(
    private val context: Context,
    private val listener: PurchasesUpdatedListener,
    private val logger: Logger,
) {
    private val connectionMutex = Mutex()

    suspend fun <T> withConnectedClient(block: suspend (BillingClient) -> T): T {
        val client = connect()
        return block(client)
    }

    private suspend fun connect() = connectionMutex.withLock {
        val client = getActiveClient()
        logger.info("Billing client connected: ${client.isReady}")
        if (!client.isReady) {
            val isConnectionEstablished = setupBillingClient(client)
            if (!isConnectionEstablished) {
                client.endConnection()
            }
        }
        client
    }

    private suspend fun setupBillingClient(client: BillingClient): Boolean {
        logger.info("Connecting to billing client")
        return suspendCancellableCoroutine<Boolean> { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    logger.info("Billing setup finished: $billingResult")
                    try {
                        continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    } catch (e: IllegalStateException) {
                        logger.error("Failed to dispatch billing connection client setup", e)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    logger.warning("Billing client disconnected")
                    client.endConnection()
                    try {
                        continuation.resume(false)
                    } catch (e: IllegalStateException) {
                        logger.error("Failed to dispatch billing client disconnection", e)
                    }
                }
            })
        }
    }

    private var billingClient: BillingClient? = null

    private fun getActiveClient(): BillingClient {
        val currentClient = billingClient
        return if (currentClient == null || !currentClient.isReady) {
            if (currentClient != null) {
                currentClient.endConnection()
            }
            createBillingClient().also { billingClient = it }
        } else {
            currentClient
        }
    }

    private fun createBillingClient(): BillingClient {
        val params = PendingPurchasesParams.newBuilder()
            .enablePrepaidPlans()
            .enableOneTimeProducts()
            .build()
        return BillingClient.newBuilder(context)
            .enablePendingPurchases(params)
            .setListener(listener)
            .build()
    }
}
