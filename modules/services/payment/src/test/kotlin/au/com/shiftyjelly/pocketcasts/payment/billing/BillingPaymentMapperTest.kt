package au.com.shiftyjelly.pocketcasts.payment.billing

import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.TestLogger
import com.android.billingclient.api.GoogleOfferDetails
import com.android.billingclient.api.GooglePricingPhase
import com.android.billingclient.api.GoogleProductDetails
import com.android.billingclient.api.GooglePurchase
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class BillingPaymentMapperTest {
    private val logger = TestLogger()
    private val mapper = BillingPaymentMapper(logger)

    @Test
    fun `map product`() {
        assertNotNull(mapper.toProduct(GoogleProductDetails()))
    }

    @Test
    fun `no errors are logged when product is mapped successfully`() {
        mapper.toProduct(GoogleProductDetails())

        logger.assertNoLogs()
    }

    @Test
    fun `map base product properties`() {
        val googleProduct = GoogleProductDetails(
            productId = "ID 1234",
            name = "Cool product",
        )

        val product = mapper.toProduct(googleProduct)!!

        assertEquals("ID 1234", product.id)
        assertEquals("Cool product", product.name)
    }

    @Test
    fun `map product without offers`() {
        val googleProduct = GoogleProductDetails(
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(
                    basePlanId = "Base plan ID",
                    offerId = null,
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            priceAmountMicros = 200_500_000,
                            priceCurrencyCode = "AUD",
                            formattedPrice = "$200.50",
                            billingPeriod = "P1M",
                            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                            billingCycleCount = 0,
                        ),
                    ),
                    offerTags = listOf("Tag", "Another tag"),
                ),
            ),
        )

        val basePlan = mapper.toProduct(googleProduct)!!.pricingPlans.basePlan

        assertEquals("Base plan ID", basePlan.planId)
        assertEquals(listOf("Tag", "Another tag"), basePlan.tags)
        assertEquals(1, basePlan.pricingPhases.size)

        val pricingPhase = basePlan.pricingPhases[0]
        assertEquals(
            Price(200.5.toBigDecimal().setScale(6), "AUD", "$200.50"),
            pricingPhase.price,
        )
        assertEquals(
            PricingSchedule(PricingSchedule.RecurrenceMode.Infinite, PricingSchedule.Period.Monthly, periodCount = 1),
            pricingPhase.schedule,
        )
    }

    @Test
    fun `map product with offers`() {
        val googleProduct = GoogleProductDetails(
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(offerId = null),
                GoogleOfferDetails(
                    basePlanId = "Offer base plan ID",
                    offerId = "Offer ID",
                    offerTags = listOf("Offer Tag", "Another offer tag"),
                ),
            ),
        )

        val offerPlans = mapper.toProduct(googleProduct)!!.pricingPlans.offerPlans
        assertEquals(1, offerPlans.size)
        val offerPlan = offerPlans[0]

        assertEquals("Offer base plan ID", offerPlan.planId)
        assertEquals("Offer ID", offerPlan.offerId)
        assertEquals(listOf("Offer Tag", "Another offer tag"), offerPlan.tags)
    }

    @Test
    fun `map product prices`() {
        val googleProduct = GoogleProductDetails(
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(offerId = null),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            priceAmountMicros = 10_000_000,
                            priceCurrencyCode = "USD",
                            formattedPrice = "$10.00",
                        ),
                        GooglePricingPhase(
                            priceAmountMicros = 15_000_000,
                            priceCurrencyCode = "EUR",
                            formattedPrice = "15.00 €",
                        ),
                        GooglePricingPhase(
                            priceAmountMicros = 20_000_000,
                            priceCurrencyCode = "PLN",
                            formattedPrice = "20.00 zł",
                        ),
                    ),
                ),
            ),
        )

        val prices = mapper.toProduct(googleProduct)!!.pricingPlans
            .offerPlans
            .flatMap { it.pricingPhases }
            .map { it.price }

        assertEquals(
            listOf(
                Price(10.toBigDecimal().setScale(6), "USD", "$10.00"),
                Price(15.toBigDecimal().setScale(6), "EUR", "15.00 €"),
                Price(20.toBigDecimal().setScale(6), "PLN", "20.00 zł"),
            ),
            prices,
        )
    }

    @Test
    fun `map product pricing schedules`() {
        val googleProduct = GoogleProductDetails(
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(offerId = null),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P1M",
                            billingCycleCount = 0,
                            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                        ),
                    ),
                ),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P1M",
                            billingCycleCount = 0,
                            recurrenceMode = RecurrenceMode.NON_RECURRING,
                        ),
                    ),
                ),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P1M",
                            billingCycleCount = 1,
                            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                        ),
                    ),
                ),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P1M",
                            billingCycleCount = 2,
                            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                        ),
                    ),
                ),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P2M",
                            billingCycleCount = 0,
                            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                        ),
                    ),
                ),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P1W",
                            billingCycleCount = 0,
                            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                        ),
                    ),
                ),
                GoogleOfferDetails(
                    offerId = "ID",
                    pricingPhases = listOf(
                        GooglePricingPhase(
                            billingPeriod = "P3Y",
                            billingCycleCount = 0,
                            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                        ),
                    ),
                ),
            ),
        )

        val pricingSchedules = mapper.toProduct(googleProduct)!!.pricingPlans
            .offerPlans
            .flatMap { it.pricingPhases }
            .map { it.schedule }

        assertEquals(
            listOf(
                PricingSchedule(
                    periodCount = 1,
                    period = PricingSchedule.Period.Monthly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.Infinite,
                ),
                PricingSchedule(
                    periodCount = 1,
                    period = PricingSchedule.Period.Monthly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.NonRecurring,
                ),
                PricingSchedule(
                    periodCount = 1,
                    period = PricingSchedule.Period.Monthly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.Recurring(1),
                ),
                PricingSchedule(
                    periodCount = 1,
                    period = PricingSchedule.Period.Monthly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.Recurring(2),
                ),
                PricingSchedule(
                    periodCount = 2,
                    period = PricingSchedule.Period.Monthly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.Infinite,
                ),
                PricingSchedule(
                    periodCount = 1,
                    period = PricingSchedule.Period.Weekly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.Infinite,
                ),
                PricingSchedule(
                    periodCount = 3,
                    period = PricingSchedule.Period.Yearly,
                    recurrenceMode = PricingSchedule.RecurrenceMode.Infinite,
                ),
            ),
            pricingSchedules,
        )
    }

    @Test
    fun `do not map product with unknown type`() {
        val googleProduct = GoogleProductDetails(
            productId = "Product ID",
            type = "foo",
        )

        assertNull(mapper.toProduct(googleProduct))
        logger.assertWarnings("Unrecognized product type 'foo' in {productId=Product ID}")
    }

    @Test
    fun `do not map product with no subscription offers`() {
        val googleProduct = GoogleProductDetails(
            productId = "Product ID",
            subscriptionOfferDetails = null,
        )

        assertNull(mapper.toProduct(googleProduct))
        logger.assertWarnings("No subscription offers in {productId=Product ID}")
    }

    @Test
    fun `do not map product with empty subscription offers`() {
        val googleProduct = GoogleProductDetails(
            productId = "Product ID",
            subscriptionOfferDetails = emptyList(),
        )

        assertNull(mapper.toProduct(googleProduct))
        logger.assertWarnings("No subscription offers in {productId=Product ID}")
    }

    @Test
    fun `do not map product with no base offer`() {
        val googleProduct = GoogleProductDetails(
            productId = "Product ID",
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(offerId = "Offer ID"),
            ),
        )

        assertNull(mapper.toProduct(googleProduct))
        logger.assertWarnings("No single base offer in {productId=Product ID}")
    }

    @Test
    fun `do not map product with multiple base offer`() {
        val googleProduct = GoogleProductDetails(
            productId = "Product ID",
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(),
                GoogleOfferDetails(),
            ),
        )

        assertNull(mapper.toProduct(googleProduct))
        logger.assertWarnings("No single base offer in {productId=Product ID}")
    }

    @Test
    fun `do not map product with unknown recurrence mode`() {
        val googleProduct = GoogleProductDetails(
            productId = "Product ID",
            subscriptionOfferDetails = listOf(
                GoogleOfferDetails(),
                GoogleOfferDetails(
                    basePlanId = "Base plan ID",
                    offerId = "Offer ID",
                    pricingPhases = listOf(GooglePricingPhase(recurrenceMode = -100)),
                ),
            ),
        )

        assertNull(mapper.toProduct(googleProduct))
        logger.assertWarnings("Unrecognized recurrence mode '-100' in {basePlanId=Base plan ID, offerId=Offer ID, productId=Product ID}")
    }

    @Test
    fun `do not map product with invalid billing duration`() {
        val durations = listOf("1M", "D1M", "PM", "P-1M", "P1U", "P1MY", "P1")
        val googleProducts = durations.map { duration ->
            GoogleProductDetails(
                productId = "Product ID",
                subscriptionOfferDetails = listOf(
                    GoogleOfferDetails(
                        basePlanId = "Base plan ID",
                        pricingPhases = listOf(GooglePricingPhase(billingPeriod = duration)),
                    ),
                ),
            )
        }

        val products = googleProducts.map(mapper::toProduct)

        assertTrue(products.all { it == null })
        logger.assertWarnings(
            "Missing billing period duration designator in {basePlanId=Base plan ID, productId=Product ID, rawDuration=1M}",
            "Missing billing period duration designator in {basePlanId=Base plan ID, productId=Product ID, rawDuration=D1M}",
            "Invalid billing period interval count '' in {basePlanId=Base plan ID, productId=Product ID, rawDuration=PM}",
            "Invalid billing period interval count '' in {basePlanId=Base plan ID, productId=Product ID, rawDuration=P-1M}",
            "Unrecognized billing interval period designator 'U' in {basePlanId=Base plan ID, productId=Product ID, rawDuration=P1U}",
            "Unrecognized billing interval period designator 'MY' in {basePlanId=Base plan ID, productId=Product ID, rawDuration=P1MY}",
            "Unrecognized billing interval period designator '' in {basePlanId=Base plan ID, productId=Product ID, rawDuration=P1}",
        )
    }

    @Test
    fun `map purchase`() {
        assertNotNull(mapper.toPurchase(GooglePurchase()))
    }

    @Test
    fun `no errors are logged when purchase is mapped`() {
        mapper.toPurchase(GooglePurchase())

        logger.assertNoLogs()
    }

    @Test
    fun `map base purchase properties`() {
        val googlePurchase = GooglePurchase(
            orderId = "Order ID",
            purchaseToken = "Purchase token",
            productIds = listOf("Product ID 1", "Product ID 2"),
            isAcknowledged = false,
            isAutoRenewing = true,
            isPurchased = true,
        )

        val purchase = mapper.toPurchase(googlePurchase)

        assertEquals(PurchaseState.Purchased("Order ID"), purchase.state)
        assertEquals("Purchase token", purchase.token)
        assertEquals(listOf("Product ID 1", "Product ID 2"), purchase.productIds)
        assertEquals(false, purchase.isAcknowledged)
        assertEquals(true, purchase.isAutoRenewing)
    }

    @Test
    fun `map pending purchase state`() {
        val googlePurchase = GooglePurchase(isPurchased = false)

        val purchase = mapper.toPurchase(googlePurchase)

        assertEquals(PurchaseState.Pending, purchase.state)
    }

    @Test
    fun `map purchase with purchased state and without order ID to unspecified state`() {
        val googlePurchase = GooglePurchase(isPurchased = true, orderId = null)

        val purchase = mapper.toPurchase(googlePurchase)

        assertEquals(PurchaseState.Unspecified, purchase.state)
    }
}
