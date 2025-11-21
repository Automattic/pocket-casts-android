package au.com.shiftyjelly.pocketcasts.payment.billing

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.TestListener
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.ReplacementMode
import com.android.billingclient.api.createGoogleOfferDetails
import com.android.billingclient.api.createGoogleProductDetails
import com.android.billingclient.api.createGooglePurchase
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
        private val listener = TestListener()
        private val mapper = BillingPaymentMapper(setOf(listener))

        @Test
        fun `create billing params`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
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
                createGooglePurchase(
                    orderId = "order-id-1",
                    purchaseToken = "purchase-token-1",
                    productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
                    isAcknowledged = true,
                    isAutoRenewing = true,
                ),
                createGooglePurchase(
                    orderId = "order-id-2",
                    purchaseToken = "purchase-token-2",
                    productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
                    isAcknowledged = true,
                    isAutoRenewing = false,
                ),
                createGooglePurchase(
                    orderId = "order-id-2",
                    purchaseToken = "purchase-token-3",
                    productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
                    isAcknowledged = false,
                    isAutoRenewing = true,
                ),
                createGooglePurchase(
                    orderId = "order-id-2",
                    purchaseToken = "purchase-token-3",
                    productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
                    isAcknowledged = false,
                    isAutoRenewing = false,
                ),
            )
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Yearly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
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
                    BillingFlowRequest.SubscriptionUpdateQuery(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID, ReplacementMode.CHARGE_FULL_PRICE),
                ),
                request,
            )
        }

        @Test
        fun `log too many matching products`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "${planKey.name}-offer-token",
                    ),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product, product), purchases = emptyList())

            listener.assertMessages(
                "Found multiple matching products in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log no matching products`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)

            mapper.toBillingFlowRequest(planKey, productDetails = emptyList(), purchases = emptyList())

            listener.assertMessages(
                "Found no matching products in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log too many matching offers`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "token-1",
                    ),
                    createGoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "token-2",
                    ),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            listener.assertMessages(
                "Found multiple matching offers in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log no matching offers`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = emptyList(),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            listener.assertMessages(
                "Found no matching offers in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log too many active purchases`() {
            val currentPurchases = listOf(
                createGooglePurchase(
                    orderId = "order-id-1",
                    productIds = listOf("product-id-1"),
                ),
                createGooglePurchase(
                    orderId = "order-id-2",
                    productIds = listOf("product-id-2"),
                ),
            )
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), currentPurchases)

            listener.assertMessages(
                "Found more than one active purchase in {purchases=order-id-1: [product-id-1], order-id-2: [product-id-2]}",
            )
        }

        @Test
        fun `log too many products in a a purchase`() {
            val currentPurchases = listOf(
                createGooglePurchase(
                    orderId = "order-id",
                    productIds = listOf("product-id-1", "product-id-2"),
                ),
            )
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), currentPurchases)

            listener.assertMessages(
                "Active purchase should have only a single product in {orderId=order-id, products=[product-id-1, product-id-2]}",
            )
        }

        @Test
        fun `do not log anything when mapped succesfully`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            listener.assertMessages()
        }
    }

    @Config(manifest = Config.NONE)
    @RunWith(ParameterizedRobolectricTestRunner::class)
    class WithoutActivePurchase(
        private val tier: SubscriptionTier,
        private val billingCycle: BillingCycle,
    ) {
        private val mapper = BillingPaymentMapper(listeners = emptySet())

        @Test
        fun `map base plan to billing flow request`() {
            val planKey = SubscriptionPlan.Key(tier, billingCycle, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = planKey.offerId,
                        offerIdToken = "${planKey.name}-offer-token",
                    ),
                    createGoogleOfferDetails(
                        basePlanId = planKey.basePlanId,
                        offerId = "random-offer-id-${planKey.name}",
                        offerIdToken = "random-offer-token-1-${planKey.name}",
                    ),
                    createGoogleOfferDetails(
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
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product, product), purchases = emptyList())

            assertNull(request)
        }

        @Test
        fun `do not map base plan to billing flow request with multiple matching offers`() {
            val planKey = SubscriptionPlan.Key(tier, billingCycle, offer = null)
            val product = createGoogleProductDetails(
                productId = planKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                    createGoogleOfferDetails(basePlanId = planKey.basePlanId, offerId = planKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertNull(request)
        }

        companion object {
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
            fun params() = listOf(
                arrayOf(SubscriptionTier.Plus, BillingCycle.Monthly),
                arrayOf(SubscriptionTier.Plus, BillingCycle.Yearly),
                arrayOf(SubscriptionTier.Patron, BillingCycle.Monthly),
                arrayOf(SubscriptionTier.Patron, BillingCycle.Yearly),
            )
        }
    }

    @Config(manifest = Config.NONE)
    @RunWith(ParameterizedRobolectricTestRunner::class)
    class WithActivePurchase(
        private val fromTier: SubscriptionTier,
        private val fromBillingCycle: BillingCycle,
        private val toTier: SubscriptionTier,
        private val toBillingCycle: BillingCycle,
        private val expectedReplacementMode: Int?,
    ) {
        private val mapper = BillingPaymentMapper(listeners = emptySet())

        @Test
        fun `create billing request with correct replacement mode`() {
            val currentProductId = SubscriptionPlan.productId(fromTier, fromBillingCycle)
            val currentPurchase = createGooglePurchase(
                productIds = listOf(currentProductId),
            )
            val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer = null)
            val product = createGoogleProductDetails(
                productId = newPlanKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = newPlanKey.basePlanId, offerId = newPlanKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), listOf(currentPurchase))

            if (expectedReplacementMode == null) {
                assertNotNull(request)
                assertNull(request?.subscriptionUpdateQuery)
            } else {
                assertEquals(
                    BillingFlowRequest.SubscriptionUpdateQuery(currentProductId, expectedReplacementMode),
                    request?.subscriptionUpdateQuery,
                )
            }
        }

        @Test
        fun `create billing request with no replacement mode when there are multiple active purchases`() {
            val currentPurchases = listOf(
                createGooglePurchase(
                    purchaseToken = "purchase-token-1",
                    productIds = listOf(
                        SubscriptionPlan.productId(fromTier, fromBillingCycle),
                    ),
                ),
                createGooglePurchase(
                    purchaseToken = "purchase-token-2",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.entries.random(), BillingCycle.entries.random()),
                    ),
                ),
            )
            val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer = null)
            val product = createGoogleProductDetails(
                productId = newPlanKey.productId,
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = newPlanKey.basePlanId, offerId = newPlanKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), currentPurchases)

            assertNotNull(request)
            assertNull(request?.subscriptionUpdateQuery)
        }

        @Test
        fun `create billing request with full price replacement mode for offers`() {
            SubscriptionOffer.entries.forEach { offer ->
                val currentProductId = SubscriptionPlan.productId(fromTier, fromBillingCycle)
                val currentPurchase = createGooglePurchase(
                    productIds = listOf(currentProductId),
                )
                val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer)
                val product = createGoogleProductDetails(
                    productId = newPlanKey.productId,
                    subscriptionOfferDetails = listOf(
                        createGoogleOfferDetails(basePlanId = newPlanKey.basePlanId, offerId = newPlanKey.offerId),
                    ),
                )

                val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), listOf(currentPurchase))

                assertEquals(
                    BillingFlowRequest.SubscriptionUpdateQuery(currentProductId, ReplacementMode.CHARGE_FULL_PRICE),
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
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    null,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    null,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    null,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    null,
                ),
            )
        }
    }
}

private val SubscriptionPlan.Key.name
    get() = buildString {
        append(tier)
        append(' ')
        append(billingCycle)
        if (offer != null) {
            append(' ')
            append(offer)
        }
    }
