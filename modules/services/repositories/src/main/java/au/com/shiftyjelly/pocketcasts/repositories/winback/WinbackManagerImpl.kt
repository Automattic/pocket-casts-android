package au.com.shiftyjelly.pocketcasts.repositories.winback

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.payment.billing.isOk
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow

class WinbackManagerImpl @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val referralManager: ReferralManager,
) : WinbackManager {
    override suspend fun loadProducts() = subscriptionManager.loadProducts()

    override suspend fun loadPurchases() = subscriptionManager.loadPurchases()

    override suspend fun changeProduct(
        currentPurchase: Purchase,
        currentPurchaseProductId: String,
        newProduct: ProductDetails,
        newProductOfferToken: String,
        activity: Activity,
    ): PurchaseEvent {
        val startResult = subscriptionManager.changeProduct(
            currentPurchase = currentPurchase,
            currentPurchaseProductId = currentPurchaseProductId,
            newProduct = newProduct,
            newProductOfferToken = newProductOfferToken,
            activity = activity,
        )
        return if (startResult.isOk()) {
            subscriptionManager.observePurchaseEvents().asFlow().first()
        } else {
            PurchaseEvent.Failure(
                "Failed to start change product flow. ${startResult.debugMessage}",
                startResult.responseCode,
            )
        }
    }

    override suspend fun getWinbackOffer() = when (val result = referralManager.getWinbackResponse()) {
        is ReferralResult.SuccessResult -> result.body
        is ReferralResult.EmptyResult -> null
        is ReferralResult.ErrorResult -> null
    }

    override suspend fun claimWinbackOffer(
        currentPurchase: Purchase,
        winbackProduct: ProductDetails,
        winbackOfferToken: String,
        winbackClaimCode: String,
        activity: Activity,
    ): PurchaseEvent {
        val startResult = subscriptionManager.claimWinbackOffer(
            currentPurchase = currentPurchase,
            winbackProduct = winbackProduct,
            winbackOfferToken = winbackOfferToken,
            activity = activity,
        )
        return if (startResult.isOk()) {
            val purchaseEvent = subscriptionManager.observePurchaseEvents().asFlow().first()
            if (purchaseEvent is PurchaseEvent.Success) {
                /**
                 * Successfully redeeming a referral code means that a user is no longer eligible for the Winback offer.
                 * Failure to redeem the code means that the Winback offer remains available to the user.
                 *
                 * However, whether the code was redeemed or not does not affect the purchase itself.
                 * It only matters for displaying the Winback offer to the user.
                 *
                 * If we forwarded the error, the user would see an error in a snackbar or some other form.
                 * But their purchase would still be processed and added to their subscription.
                 * They would still see the "Claim offer" button, allowing them to claim the offer indefinitely,
                 * as long as the redeem call fails.
                 *
                 * For this reason, we ignore the result of redeeming the referral code.
                 */
                referralManager.redeemReferralCode(winbackClaimCode)
            }
            purchaseEvent
        } else {
            PurchaseEvent.Failure(
                "Failed to start change product flow. ${startResult.debugMessage}",
                startResult.responseCode,
            )
        }
    }
}
