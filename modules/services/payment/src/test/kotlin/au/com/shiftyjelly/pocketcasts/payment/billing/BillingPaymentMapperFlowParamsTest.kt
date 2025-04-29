package au.com.shiftyjelly.pocketcasts.payment.billing

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionBillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.TestLogger
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.GoogleOfferDetails
import com.android.billingclient.api.GoogleProductDetails
import com.android.billingclient.api.GooglePurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(Enclosed::class)
class BillingPaymentMapperFlowParamsTest {
    @Config(manifest = Config.NONE)
    @RunWith(RobolectricTestRunner::class)
    class GeneralBehavior {
        private val logger = TestLogger()
        private val mapper = BillingPaymentMapper(logger)

        @Test
        fun `create billing params`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "offer-token",
                    ),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertEquals(
                BillingFlowRequest(
                    BillingFlowRequest.ProductQuery(product, "offer-token"),
                    subscriptionUpdateQuery = null,
                ),
                request,
            )
        }

        @Test
        fun `create billing params with only single active purchase`() {
            val currentPurchases = listOf(
                GooglePurchase(
                    orderId = "order-id-1",
                    purchaseToken = "purchase-token-1",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly),
                    ),
                    isAcknowledged = true,
                    isAutoRenewing = true,
                ),
                GooglePurchase(
                    orderId = "order-id-2",
                    purchaseToken = "purchase-token-2",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly),
                    ),
                    isAcknowledged = true,
                    isAutoRenewing = false,
                ),
                GooglePurchase(
                    orderId = "order-id-2",
                    purchaseToken = "purchase-token-3",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly),
                    ),
                    isAcknowledged = false,
                    isAutoRenewing = true,
                ),
                GooglePurchase(
                    orderId = "order-id-2",
                    purchaseToken = "purchase-token-3",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly),
                    ),
                    isAcknowledged = false,
                    isAutoRenewing = false,
                ),
            )
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Yearly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "offer-token",
                    ),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), currentPurchases)

            assertEquals(
                BillingFlowRequest(
                    BillingFlowRequest.ProductQuery(product, "offer-token"),
                    BillingFlowRequest.SubscriptionUpdateQuery("purchase-token-1", ReplacementMode.CHARGE_FULL_PRICE),
                ),
                request,
            )
        }

        @Test
        fun `log too many matching products`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "${planKey.name}-offer-token",
                    ),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product, product), purchases = emptyList())

            logger.assertWarnings(
                "Found multiple matching products in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log no matching products`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)

            mapper.toBillingFlowRequest(planKey, productDetails = emptyList(), purchases = emptyList())

            logger.assertWarnings(
                "Found no matching products in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log too many matching offers`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "token-1",
                    ),
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "token-2",
                    ),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            logger.assertWarnings(
                "Found multiple matching offers in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log no matching offers`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = emptyList(),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            logger.assertWarnings(
                "Found no matching offers in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log too many active purchases`() {
            val currentPurchases = listOf(
                GooglePurchase(
                    orderId = "order-id-1",
                    productIds = listOf("product-id-1"),
                ),
                GooglePurchase(
                    orderId = "order-id-2",
                    productIds = listOf("product-id-2"),
                ),
            )
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), currentPurchases)

            logger.assertWarnings(
                "Found more than one active purchase in {purchases=order-id-1: [product-id-1], order-id-2: [product-id-2]}",
            )
        }

        @Test
        fun `log too many products in a a purchase`() {
            val currentPurchases = listOf(
                GooglePurchase(
                    orderId = "order-id",
                    productIds = listOf("product-id-1", "product-id-2"),
                ),
            )
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), currentPurchases)

            logger.assertWarnings(
                "Active purchase should have only a single product in {orderId=order-id, products=[product-id-1, product-id-2]}",
            )
        }

        @Test
        fun `do not log anything when mapped succesfully`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            logger.assertNoLogs()
        }
    }

    @Config(manifest = Config.NONE)
    @RunWith(ParameterizedRobolectricTestRunner::class)
    class WithoutActivePurchase(
        private val tier: SubscriptionTier,
        private val billingCycle: SubscriptionBillingCycle,
    ) {
        private val mapper = BillingPaymentMapper(TestLogger())

        @Test
        fun `map base plan to billing flow request`() {
            val planKey = SubscriptionPlan.Key(tier, billingCycle, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "${planKey.name}-offer-token",
                    ),
                    GoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = "random-offer-id-${planKey.name}",
                        offerIdToken = "random-offer-token-1-${planKey.name}",
                    ),
                    GoogleOfferDetails(
                        basePlanId = "random-base-plan-id-${planKey.name}",
                        offerId = planKey.offerId,
                        offerIdToken = "random-offer-token-2-${planKey.name}",
                    ),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertEquals(
                BillingFlowRequest(
                    productQuery = BillingFlowRequest.ProductQuery(
                        product = product,
                        offerToken = "${planKey.name}-offer-token",
                    ),
                    subscriptionUpdateQuery = null,
                ),
                request,
            )
        }

        @Test
        fun `do not map base plan to billing flow request with multiple matching products`() {
            val planKey = SubscriptionPlan.Key(tier, billingCycle, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product, product), purchases = emptyList())

            assertNull(request)
        }

        @Test
        fun `do not map base plan to billing flow request with multiple matching offers`() {
            val planKey = SubscriptionPlan.Key(tier, billingCycle, offer = null)
            val product = GoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                    GoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertNull(request)
        }

        companion object {
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
            fun params() = listOf(
                arrayOf(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly),
                arrayOf(SubscriptionTier.Plus, SubscriptionBillingCycle.Yearly),
                arrayOf(SubscriptionTier.Patron, SubscriptionBillingCycle.Monthly),
                arrayOf(SubscriptionTier.Patron, SubscriptionBillingCycle.Yearly),
            )
        }
    }

    @Config(manifest = Config.NONE)
    @RunWith(ParameterizedRobolectricTestRunner::class)
    class WithActivePurchase(
        private val fromTier: SubscriptionTier,
        private val fromBillingCycle: SubscriptionBillingCycle,
        private val toTier: SubscriptionTier,
        private val toBillingCycle: SubscriptionBillingCycle,
        private val expectedReplacementMode: Int?,
    ) {
        private val mapper = BillingPaymentMapper(TestLogger())

        @Test
        fun `create billing request with correct replacement mode`() {
            val currentPurchase = GooglePurchase(
                purchaseToken = "purchase-token",
                productIds = listOf(SubscriptionPlan.productId(fromTier, fromBillingCycle)),
            )
            val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer = null)
            val product = GoogleProductDetails(
                productId = newPlanKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = newPlanKey.basePlanId, offerId = newPlanKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), listOf(currentPurchase))

            if (expectedReplacementMode == null) {
                assertNotNull(request)
                assertNull(request?.subscriptionUpdateQuery)
            } else {
                assertEquals(
                    BillingFlowRequest.SubscriptionUpdateQuery("purchase-token", expectedReplacementMode),
                    request?.subscriptionUpdateQuery,
                )
            }
        }

        @Test
        fun `create billing request with no replacement mode when there are multiple active purchases`() {
            val currentPurchases = listOf(
                GooglePurchase(
                    purchaseToken = "purchase-token-1",
                    productIds = listOf(
                        SubscriptionPlan.productId(fromTier, fromBillingCycle),
                    ),
                ),
                GooglePurchase(
                    purchaseToken = "purchase-token-2",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.entries.random(), SubscriptionBillingCycle.entries.random()),
                    ),
                ),
            )
            val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer = null)
            val product = GoogleProductDetails(
                productId = newPlanKey.productId,
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(basePlanId = newPlanKey.basePlanId, offerId = newPlanKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), currentPurchases)

            assertNotNull(request)
            assertNull(request?.subscriptionUpdateQuery)
        }

        @Test
        fun `craete billing request with full price replacement mode for offers`() {
            SubscriptionOffer.entries.forEach { offer ->
                val currentPurchase = GooglePurchase(
                    purchaseToken = "purchase-token",
                    productIds = listOf(SubscriptionPlan.productId(fromTier, fromBillingCycle)),
                )
                val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer)
                val product = GoogleProductDetails(
                    productId = newPlanKey.productId,
                    subscriptionOfferDetails = listOf(
                        GoogleOfferDetails(basePlanId = newPlanKey.basePlanId, offerId = newPlanKey.offerId),
                    ),
                )

                val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), listOf(currentPurchase))

                assertEquals(
                    BillingFlowRequest.SubscriptionUpdateQuery("purchase-token", ReplacementMode.CHARGE_FULL_PRICE),
                    request?.subscriptionUpdateQuery,
                )
            }
        }

        companion object {
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "From: {0} {1}, To: {2} {3}")
            fun params() = listOf<Array<Any?>>(
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    null,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    null,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    null,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    SubscriptionBillingCycle.Yearly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    SubscriptionBillingCycle.Yearly,
                    null,
                ),
            )
        }
    }
}

private val SubscriptionPlan.Key.name get() = buildString {
    append(tier)
    append(' ')
    append(billingCycle)
    if (offer != null) {
        append(' ')
        append(offer)
    }
}
