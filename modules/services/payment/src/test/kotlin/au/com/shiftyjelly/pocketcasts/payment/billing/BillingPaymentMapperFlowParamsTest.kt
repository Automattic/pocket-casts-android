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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
                        offerId = planKey.offerId,
                        offerIdToken = "${planKey.tier}-${planKey.billingCycle}-offer-token",
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
                        offerId = planKey.offerId,
                        offerIdToken = "token-1",
                    ),
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
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
                productId = requireNotNull(planKey.productId),
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(planKey.basePlanId), offerId = planKey.offerId),
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(planKey.basePlanId), offerId = planKey.offerId),
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(planKey.basePlanId), offerId = planKey.offerId),
                ),
            )

            mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            listener.assertMessages()
        }

        @Test
        fun `log unsupported plan combination for Plus Monthly installment`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null, isInstallment = true)
            // productId and basePlanId will be null for this unsupported combination
            val product = createGoogleProductDetails(
                productId = "com.pocketcasts.plus.monthly",
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = "p1m", offerId = null),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertNull(request)
            listener.assertMessages(
                "Unsupported plan combination in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
        }

        @Test
        fun `log unsupported plan combination for Patron Monthly installment`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Patron, BillingCycle.Monthly, offer = null, isInstallment = true)
            // productId and basePlanId will be null for this unsupported combination
            val product = createGoogleProductDetails(
                productId = "com.pocketcasts.monthly.patron",
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = "patron-monthly", offerId = null),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertNull(request)
            listener.assertMessages(
                "Unsupported plan combination in {billingCycle=Monthly, offer=null, tier=Patron}",
            )
        }

        @Test
        fun `log unsupported plan combination for Patron Yearly installment`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Patron, BillingCycle.Yearly, offer = null, isInstallment = true)
            // productId and basePlanId will be null for this unsupported combination
            val product = createGoogleProductDetails(
                productId = "com.pocketcasts.yearly.patron",
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = "patron-yearly", offerId = null),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertNull(request)
            listener.assertMessages(
                "Unsupported plan combination in {billingCycle=Yearly, offer=null, tier=Patron}",
            )
        }

        @Test
        fun `return null for unsupported plan combination without logging errors when no products provided`() {
            val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null, isInstallment = true)

            val request = mapper.toBillingFlowRequest(planKey, productDetails = emptyList(), purchases = emptyList())

            assertNull(request)
            listener.assertMessages(
                "Unsupported plan combination in {billingCycle=Monthly, offer=null, tier=Plus}",
            )
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
                        offerId = planKey.offerId,
                        offerIdToken = "${planKey.tier}-${planKey.billingCycle}-offer-token",
                    ),
                    createGoogleOfferDetails(
                        basePlanId = requireNotNull(planKey.basePlanId),
                        offerId = "random-offer-id-${planKey.tier}-${planKey.billingCycle}",
                        offerIdToken = "random-offer-token-1-${planKey.tier}-${planKey.billingCycle}",
                    ),
                    createGoogleOfferDetails(
                        basePlanId = "random-base-plan-id-${planKey.tier}-${planKey.billingCycle}",
                        offerId = planKey.offerId,
                        offerIdToken = "random-offer-token-2-${planKey.tier}-${planKey.billingCycle}",
                    ),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertEquals(
                BillingFlowRequest(
                    productQuery = BillingFlowRequest.ProductQuery(
                        product = product,
                        offerToken = "${planKey.tier}-${planKey.billingCycle}-offer-token",
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
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(planKey.basePlanId), offerId = planKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product, product), purchases = emptyList())

            assertNull(request)
        }

        @Test
        fun `do not map base plan to billing flow request with multiple matching offers`() {
            val planKey = SubscriptionPlan.Key(tier, billingCycle, offer = null)
            val product = createGoogleProductDetails(
                productId = requireNotNull(planKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(planKey.basePlanId), offerId = planKey.offerId),
                    createGoogleOfferDetails(basePlanId = requireNotNull(planKey.basePlanId), offerId = planKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(planKey, listOf(product), purchases = emptyList())

            assertNull(request)
        }

        companion object {
            @JvmStatic
            @Suppress("unused")
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
        private val fromIsInstallment: Boolean,
        private val toIsInstallment: Boolean,
    ) {
        private val mapper = BillingPaymentMapper(listeners = emptySet())

        @Test
        fun `create billing request with correct replacement mode`() {
            val currentProductId = SubscriptionPlan.productId(fromTier, fromBillingCycle, fromIsInstallment)!!
            val currentPurchase = createGooglePurchase(
                productIds = listOf(currentProductId),
            )
            val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer = null, isInstallment = toIsInstallment)
            val product = createGoogleProductDetails(
                productId = requireNotNull(newPlanKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(newPlanKey.basePlanId), offerId = newPlanKey.offerId),
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
                        SubscriptionPlan.productId(fromTier, fromBillingCycle, fromIsInstallment)!!,
                    ),
                ),
                createGooglePurchase(
                    purchaseToken = "purchase-token-2",
                    productIds = listOf(
                        SubscriptionPlan.productId(SubscriptionTier.entries.random(), BillingCycle.entries.random())!!,
                    ),
                ),
            )
            val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer = null, isInstallment = toIsInstallment)
            val product = createGoogleProductDetails(
                productId = requireNotNull(newPlanKey.productId),
                subscriptionOfferDetails = listOf(
                    createGoogleOfferDetails(basePlanId = requireNotNull(newPlanKey.basePlanId), offerId = newPlanKey.offerId),
                ),
            )

            val request = mapper.toBillingFlowRequest(newPlanKey, listOf(product), currentPurchases)

            assertNotNull(request)
            assertNull(request?.subscriptionUpdateQuery)
        }

        @Test
        fun `create billing request with full price replacement mode for offers`() {
            // Skip this test for installment plans since they don't support promotional offers
            if (toIsInstallment) {
                return
            }

            SubscriptionOffer.entries.forEach { offer ->
                val currentProductId = SubscriptionPlan.productId(fromTier, fromBillingCycle, fromIsInstallment)!!
                val currentPurchase = createGooglePurchase(
                    productIds = listOf(currentProductId),
                )
                val newPlanKey = SubscriptionPlan.Key(toTier, toBillingCycle, offer, isInstallment = toIsInstallment)
                val product = createGoogleProductDetails(
                    productId = requireNotNull(newPlanKey.productId),
                    subscriptionOfferDetails = listOf(
                        createGoogleOfferDetails(basePlanId = requireNotNull(newPlanKey.basePlanId), offerId = newPlanKey.offerId),
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
            @Suppress("unused")
            @ParameterizedRobolectricTestRunner.Parameters(name = "From: {0} {1} (installment={5}), To: {2} {3} (installment={6})")
            fun params() = listOf<Array<Any?>>(
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    null,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    null,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    null,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    null,
                    false,
                    false,
                ),
                // Installment plan scenarios
                // FROM Plus Yearly Installment TO other plans
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    true, // fromIsInstallment
                    false, // toIsInstallment
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    true,
                    false,
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_PRORATED_PRICE,
                    true,
                    false,
                ),
                // FROM other plans TO Plus Yearly Installment
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                    false,
                    true, // toIsInstallment
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Monthly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.CHARGE_FULL_PRICE,
                    false,
                    true,
                ),
                arrayOf(
                    SubscriptionTier.Patron,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    ReplacementMode.WITH_TIME_PRORATION,
                    false,
                    true,
                ),
                // Same plan scenarios (installment to non-installment and vice versa)
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    null,
                    true, // fromIsInstallment
                    true, // toIsInstallment
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    null,
                    false,
                    true, // toIsInstallment
                ),
                arrayOf(
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    SubscriptionTier.Plus,
                    BillingCycle.Yearly,
                    null,
                    true, // fromIsInstallment
                    false,
                ),
            )
        }
    }
}
