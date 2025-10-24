package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakePaymentDataSource : PaymentDataSource {
    var loadedProducts: List<Product> = DefaultLoadedProducts
    var loadedProductsResultCode: PaymentResultCode = PaymentResultCode.Ok

    var loadedPurchases: List<Purchase> = listOf(DefaultLoadedPurchase)
    var loadedPurchasesResultCode: PaymentResultCode = PaymentResultCode.Ok

    var billingFlowResultCode: PaymentResultCode = PaymentResultCode.Ok
    var purchasedProducts: List<Purchase> = listOf(DefaultPurchasedProduct)
    var purchasedProductsResultCode: PaymentResultCode = PaymentResultCode.Ok

    var acknowledgePurchaseResultCode: PaymentResultCode = PaymentResultCode.Ok
    var receivedPurchases = emptyList<Purchase>()
        private set

    private val _purchaseResults = MutableSharedFlow<PaymentResult<List<Purchase>>>()

    override val purchaseResults = _purchaseResults.asSharedFlow()

    override suspend fun loadProducts(): PaymentResult<List<Product>> {
        return if (loadedProductsResultCode == PaymentResultCode.Ok) {
            PaymentResult.Success(loadedProducts)
        } else {
            PaymentResult.Failure(loadedProductsResultCode, "Load products error")
        }
    }

    override suspend fun loadPurchases(): PaymentResult<List<Purchase>> {
        return if (loadedPurchasesResultCode == PaymentResultCode.Ok) {
            PaymentResult.Success(loadedPurchases)
        } else {
            PaymentResult.Failure(loadedPurchasesResultCode, "Load purchases error")
        }
    }

    override suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity): PaymentResult<Unit> {
        return if (billingFlowResultCode == PaymentResultCode.Ok) {
            val billingResult = PaymentResult.Success(Unit)
            val purchaseResult = if (purchasedProductsResultCode == PaymentResultCode.Ok) {
                PaymentResult.Success(purchasedProducts)
            } else {
                PaymentResult.Failure(purchasedProductsResultCode, "Purchase product error")
            }
            _purchaseResults.emit(purchaseResult)
            billingResult
        } else {
            PaymentResult.Failure(billingFlowResultCode, "Launch billing error")
        }
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase> {
        receivedPurchases += purchase
        return if (acknowledgePurchaseResultCode == PaymentResultCode.Ok) {
            PaymentResult.Success(purchase.copy(isAcknowledged = true))
        } else {
            PaymentResult.Failure(acknowledgePurchaseResultCode, "Acknowledge purchase error")
        }
    }

    companion object {
        val DefaultLoadedProducts
            get() = SubscriptionTier.entries.flatMap { tier ->
                BillingCycle.entries.map { billingCycle ->
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

        val DefaultLoadedPurchase
            get() = Purchase(
                state = PurchaseState.Purchased("order-id"),
                token = "purchase-token",
                productIds = listOf(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID),
                isAcknowledged = true,
                isAutoRenewing = true,
            )

        val DefaultPurchasedProduct
            get() = Purchase(
                state = PurchaseState.Purchased("order-id"),
                token = "purchase-token",
                productIds = listOf(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID),
                isAcknowledged = false,
                isAutoRenewing = true,
            )
    }
}

private val PlusMonthlyPricingPhase
    get() = PricingPhase(
        Price(3.99.toBigDecimal(), "USD", "$3.99"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 0),
    )
private val PlusYearlyPricingPhase
    get() = PricingPhase(
        Price(39.99.toBigDecimal(), "USD", "$39.99"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Yearly, periodCount = 0),
    )
private val PatronMonthlyPricingPhase
    get() = PricingPhase(
        Price(9.99.toBigDecimal(), "USD", "$9.99"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 0),
    )
private val PatronYearlyPricingPhase
    get() = PricingPhase(
        Price(99.99.toBigDecimal(), "USD", "$99.99"),
        PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Yearly, periodCount = 0),
    )

private fun productName(
    tier: SubscriptionTier,
    billingCycle: BillingCycle,
) = "$tier $billingCycle (Fake)"

private fun pricingPhases(
    tier: SubscriptionTier,
    billingCycle: BillingCycle,
    offer: SubscriptionOffer?,
): List<PricingPhase> = when (offer) {
    SubscriptionOffer.IntroOffer -> when (billingCycle) {
        BillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.5, period = PricingSchedule.Period.Yearly),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        BillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Trial -> when (billingCycle) {
        BillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.0),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        BillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Referral -> when (billingCycle) {
        BillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.0, intervalCount = 2),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> emptyList()
        }

        BillingCycle.Monthly -> emptyList()
    }

    SubscriptionOffer.Winback -> when (billingCycle) {
        BillingCycle.Monthly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusMonthlyPricingPhase.withDiscount(priceFraction = 0.5),
                PlusMonthlyPricingPhase,
            )

            SubscriptionTier.Patron -> listOf(
                PatronMonthlyPricingPhase.withDiscount(priceFraction = 0.5),
                PatronMonthlyPricingPhase,
            )
        }

        BillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(
                PlusYearlyPricingPhase.withDiscount(priceFraction = 0.5, period = PricingSchedule.Period.Yearly),
                PlusYearlyPricingPhase,
            )

            SubscriptionTier.Patron -> listOf(
                PatronYearlyPricingPhase.withDiscount(priceFraction = 0.5, period = PricingSchedule.Period.Yearly),
                PatronYearlyPricingPhase,
            )
        }
    }

    null -> when (billingCycle) {
        BillingCycle.Monthly -> when (tier) {
            SubscriptionTier.Plus -> listOf(PlusMonthlyPricingPhase)
            SubscriptionTier.Patron -> listOf(PatronMonthlyPricingPhase)
        }

        BillingCycle.Yearly -> when (tier) {
            SubscriptionTier.Plus -> listOf(PlusYearlyPricingPhase)
            SubscriptionTier.Patron -> listOf(PatronYearlyPricingPhase)
        }
    }
}

private fun PricingPhase.withDiscount(
    priceFraction: Double,
    recurrenceMode: PricingSchedule.RecurrenceMode = PricingSchedule.RecurrenceMode.Recurring(1),
    period: PricingSchedule.Period = PricingSchedule.Period.Monthly,
    intervalCount: Int = 1,
): PricingPhase {
    val newAmount = price.amount.times(priceFraction.coerceIn(0.0..1.0).toBigDecimal()).stripTrailingZeros()
    val newFormattedPrice = if (newAmount == BigDecimal.ZERO) "Free" else "$%.2f".format(Locale.ROOT, newAmount.toDouble())
    return copy(
        price = price.copy(amount = newAmount, formattedPrice = newFormattedPrice),
        schedule = PricingSchedule(recurrenceMode, period, intervalCount),
    )
}
