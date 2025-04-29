package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.android.billingclient.api.Purchase as GooglePurchase

class FakePaymentDataSource : PaymentDataSource {
    var customProductsResult: PaymentResult<List<Product>>? = null
    var customPurchases: PaymentResult<List<Purchase>>? = null
    var acknowledgePurchaseResultCode: PaymentResultCode = PaymentResultCode.Ok
    var launchBillingFlowResultCode: PaymentResultCode = PaymentResultCode.Ok

    override val purchaseResults = MutableSharedFlow<PaymentResult<List<Purchase>>>()

    override suspend fun loadProducts(): PaymentResult<List<Product>> {
        return customProductsResult ?: PaymentResult.Success(KnownProducts)
    }

    override suspend fun loadPurchases(): PaymentResult<List<Purchase>> {
        return customPurchases ?: PaymentResult.Success(emptyList())
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase> {
        return if (acknowledgePurchaseResultCode is PaymentResultCode.Ok) {
            PaymentResult.Success(purchase.copy(isAcknowledged = true))
        } else {
            PaymentResult.Failure(acknowledgePurchaseResultCode, "Error")
        }
    }

    override suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity): PaymentResult<Unit> {
        return if (launchBillingFlowResultCode is PaymentResultCode.Ok) {
            PaymentResult.Success(Unit)
        } else {
            PaymentResult.Failure(acknowledgePurchaseResultCode, "Error")
        }
    }

    // <editor-fold desc="Temporarily extracted old interface">
    override val purchaseUpdates: SharedFlow<Pair<BillingResult, List<GooglePurchase>>> = MutableSharedFlow()

    override suspend fun loadProducts(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>> = BillingResult.newBuilder().build() to emptyList()

    override suspend fun loadPurchaseHistory(
        params: QueryPurchaseHistoryParams,
    ): Pair<BillingResult, List<PurchaseHistoryRecord>> = BillingResult.newBuilder().build() to emptyList()

    override suspend fun loadPurchases(
        params: QueryPurchasesParams,
    ): Pair<BillingResult, List<GooglePurchase>> = BillingResult.newBuilder().build() to emptyList()

    override suspend fun acknowledgePurchase(
        params: AcknowledgePurchaseParams,
    ): BillingResult = BillingResult.newBuilder().build()

    override suspend fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult = BillingResult.newBuilder().build()
    // </editor-fold>
}

private val KnownProducts get() = SubscriptionTier.entries.flatMap { tier ->
    SubscriptionBillingCycle.entries.map { billingCycle ->
        Product(
            SubscriptionPlan.productId(tier, billingCycle),
            productName(tier, billingCycle),
            PricingPlans(
                PricingPlan.Base(
                    SubscriptionPlan.basePlanId(tier, billingCycle),
                    pricingPhases(tier, billingCycle, offer = null),
                    emptyList(),
                ),
                SubscriptionOffer.entries
                    .mapNotNull { offer -> offer.offerId(tier, billingCycle)?.let { offer to it } }
                    .map { (offer, offerId) ->
                        PricingPlan.Offer(
                            offerId,
                            SubscriptionPlan.basePlanId(tier, billingCycle),
                            pricingPhases(tier, billingCycle, offer),
                            emptyList(),
                        )
                    },
            ),
        )
    }
}

private val PlusMonthlyPricingPhase get() = PricingPhase(
    Price(3.99.toBigDecimal(), "USD", "$3.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Monthly, intervalCount = 0),
)
private val PlusYearlyPricingPhase get() = PricingPhase(
    Price(39.99.toBigDecimal(), "USD", "$39.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Monthly, intervalCount = 0),
)
private val PatronMonthlyPricingPhase get() = PricingPhase(
    Price(9.99.toBigDecimal(), "USD", "$9.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Monthly, intervalCount = 0),
)
private val PatronYearlyPricingPhase get() = PricingPhase(
    Price(99.99.toBigDecimal(), "USD", "$99.99"),
    BillingPeriod(BillingPeriod.Cycle.Infinite, BillingPeriod.Interval.Yearly, intervalCount = 0),
)

private fun productName(
    tier: SubscriptionTier,
    billingCycle: SubscriptionBillingCycle,
) = "$tier $billingCycle (Fake)"

private fun pricingPhases(
    tier: SubscriptionTier,
    billingCycle: SubscriptionBillingCycle,
    offer: SubscriptionOffer?,
): List<PricingPhase> = when (offer) {
    SubscriptionOffer.Trial -> when (billingCycle) {
        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.0),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        SubscriptionBillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Referral -> when (billingCycle) {
        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.0, intervalCount = 2),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        SubscriptionBillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Winback -> when (billingCycle) {
        SubscriptionBillingCycle.Monthly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusMonthlyPricingPhase.withDiscount(priceFraction = 0.5),
                PlusMonthlyPricingPhase,
            )

            SubscriptionTier.Patron -> listOf(
                PatronMonthlyPricingPhase.withDiscount(priceFraction = 0.5),
                PatronMonthlyPricingPhase,
            )
        }

        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.5, interval = BillingPeriod.Interval.Yearly),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> listOf(
                PatronYearlyPricingPhase.withDiscount(priceFraction = 0.5, interval = BillingPeriod.Interval.Yearly),
                PatronYearlyPricingPhase,
            )
        }
    }

    null -> when (billingCycle) {
        SubscriptionBillingCycle.Monthly -> when (tier) {
            SubscriptionTier.Plus -> listOf(PlusMonthlyPricingPhase)
            SubscriptionTier.Patron -> listOf(PatronMonthlyPricingPhase)
        }

        SubscriptionBillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(PlusYearlyPricingPhase)
            SubscriptionTier.Patron -> listOf(PatronYearlyPricingPhase)
        }
    }
}

private fun PricingPhase.withDiscount(
    priceFraction: Double,
    cycle: BillingPeriod.Cycle = BillingPeriod.Cycle.Recurring(1),
    interval: BillingPeriod.Interval = BillingPeriod.Interval.Monthly,
    intervalCount: Int = 1,
): PricingPhase {
    val newAmount = price.amount.times(priceFraction.coerceIn(0.0..1.0).toBigDecimal())
    val newFormattedPrice = if (newAmount == BigDecimal.ZERO) "Free" else "$%.2f".format(newAmount.toDouble())
    return copy(
        price = price.copy(amount = newAmount, formattedPrice = newFormattedPrice),
        billingPeriod = BillingPeriod(cycle, interval, intervalCount),
    )
}
