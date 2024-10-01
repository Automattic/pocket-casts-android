package com.android.billingclient.api

import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsInterceptor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProductDetailsModule {
    @Provides
    fun provideProductDetailsInterceptor(): ProductDetailsInterceptor = MockingProductDetailsInterceptor()
}

private class MockingProductDetailsInterceptor : ProductDetailsInterceptor {
    override fun intercept(result: BillingResult, details: List<ProductDetails>): Pair<BillingResult, List<ProductDetails>> {
        return if (details.isEmpty()) {
            okResponse to products
        } else {
            result to details
        }
    }

    private companion object {
        private val okResponse = BillingResult
            .newBuilder()
            .setResponseCode(OK)
            .build()
        private val products
            get() = listOf(
                yearlyTestPlusProduct,
                yearlyPlusProduct,
                yearlyPatronProduct,
                monthlyPlusProduct,
                monthlyPatronProduct,
            )
        val yearlyTestPlusProduct = ProductDetails(
            """
            |{
            |  "productId": "com.pocketcasts.plus.testfreetrialoffer",
            |  "type": "subs",
            |  "title": "Test Subscription (Pocket Casts - Podcast Player)",
            |   "name": "Test Subscription",
            |   "localizedIn": [
            |    "en-US"
            |  ],
            |  "skuDetailsToken": "AEuhp4LWRPDnJGhmqmkVkTNpFpXFGvZtBtIT33KsKWhIBQhKnGX1tJ9c6NOcRooeMAW0",
            |  "subscriptionOfferDetails": [
            |    {
            |      "offerIdToken": "AarRn8p4dWN62vQ67aqgHtzD0uaeQ41esr6+rol9mLhh5qulWuO/ChkTprmB7DBH3yjqmbPhJvP09DfZfU0p8asww04FIomfvCU9L3Ho/uOvMgsskI0u",
            |      "basePlanId": "testyearly",
            |      "offerId": "2-months-free",
            |     "pricingPhases": [
            |        {
            |          "priceAmountMicros": 0,
            |          "priceCurrencyCode": "USD",
            |         "formattedPrice": "Free",
            |          "billingPeriod": "P2M",
            |          "recurrenceMode": 2,
            |          "billingCycleCount": 1
            |        },
            |        {
            |          "priceAmountMicros": 2150000000,
            |          "priceCurrencyCode": "USD",
            |          "formattedPrice": "$39.99",
            |          "billingPeriod": "P1Y",
            |          "recurrenceMode": 1
            |        }
            |      ],
            |      "offerTags": [
            |        "referral-offer"
            |      ]
            |    },
            |    {
            |      "offerIdToken": "AarRn8ratf+yxb+9u+sc8Tm6/yODavGSOfOWHh2FDc0ZG+ltJdIcpiep5TX6bReI/WyGOxtV7t/KMdm92n/m9G0TdA==",
            |      "basePlanId": "testyearly",
            |      "pricingPhases": [
            |        {
            |          "priceAmountMicros": 2150000000,
            |          "priceCurrencyCode": "USD",
            |          "formattedPrice": "$39.99",
            |          "billingPeriod": "P1Y",
            |          "recurrenceMode": 1
            |        }
            |      ],
            |      "offerTags": []
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )
        val yearlyPlusProduct = ProductDetails(
            """
            |{
            |  "productId": "com.pocketcasts.plus.yearly",
            |  "type": "subs",
            |  "title": "Yearly (Pocket Casts - Podcast Player)",
            |  "name": "Yearly",
            |  "description": "Yearly subscription to Pocket Casts Plus",
            |  "localizedIn": [
            |    "en-US"
            |  ],
            |  "skuDetailsToken": "yearly-plus-sku-details-token",
            |  "subscriptionOfferDetails": [
            |    {
            |      "offerIdToken": "yearly-plus-offer-id-token",
            |      "basePlanId": "p1y",
            |      "pricingPhases": [
            |        {
            |          "priceAmountMicros": 420690000,
            |          "priceCurrencyCode": "USD",
            |          "formattedPrice": "$ 420.69",
            |          "billingPeriod": "P1Y",
            |          "recurrenceMode": 1
            |        }
            |      ],
            |      "offerTags": []
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )
        val yearlyPatronProduct = ProductDetails(
            """
            |{
            |  "productId": "com.pocketcasts.yearly.patron",
            |  "type": "subs",
            |  "title": "Patron Yearly (Pocket Casts - Podcast Player)",
            |  "name": "Patron Yearly",
            |  "description": "Yearly subscription to Pocket Casts Patron",
            |  "localizedIn": [
            |    "en-US"
            |  ],
            |  "skuDetailsToken": "yearly-patron-sku-details-token",
            |  "subscriptionOfferDetails": [
            |    {
            |      "offerIdToken": "yearly-patron-offer-id-token",
            |      "basePlanId": "patron-yearly",
            |      "pricingPhases": [
            |        {
            |          "priceAmountMicros": 694200000,
            |          "priceCurrencyCode": "USD",
            |          "formattedPrice": "$ 694.20",
            |          "billingPeriod": "P1Y",
            |          "recurrenceMode": 1
            |        }
            |      ],
            |      "offerTags": []
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )
        val monthlyPlusProduct = ProductDetails(
            """
            |{
            |  "productId": "com.pocketcasts.plus.monthly",
            |  "type": "subs",
            |  "title": "Monthly (Pocket Casts - Podcast Player)",
            |  "name": "Monthly",
            |  "description": "Monthly subscription to Pocket Casts Plus",
            |  "localizedIn": [
            |    "en-US"
            |  ],
            |  "skuDetailsToken": "monthly-plus-sku-details-token",
            |  "subscriptionOfferDetails": [
            |    {
            |      "offerIdToken": "monthly-plus-offer-id-token",
            |      "basePlanId": "p1m",
            |      "pricingPhases": [
            |        {
            |          "priceAmountMicros": 3500000,
            |          "priceCurrencyCode": "USD",
            |          "formattedPrice": "$ 3.50",
            |          "billingPeriod": "P1M",
            |          "recurrenceMode": 1
            |        }
            |      ],
            |      "offerTags": []
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )
        val monthlyPatronProduct = ProductDetails(
            """
            |{
            |  "productId": "com.pocketcasts.monthly.patron",
            |  "type": "subs",
            |  "title": "Patron Monthly (Pocket Casts - Podcast Player)",
            |  "name": "Monthly",
            |  "description": "Monthly subscription to Pocket Casts Patron",
            |  "localizedIn": [
            |    "en-US"
            |  ],
            |  "skuDetailsToken": "monthly-patron-sku-details-token",
            |  "subscriptionOfferDetails": [
            |    {
            |      "offerIdToken": "monthly-patron-offer-id-token",
            |      "basePlanId": "patron-monthly",
            |      "pricingPhases": [
            |        {
            |          "priceAmountMicros": 10500000,
            |          "priceCurrencyCode": "USD",
            |          "formattedPrice": "$ 10.50",
            |          "billingPeriod": "P1M",
            |          "recurrenceMode": 1
            |        }
            |      ],
            |      "offerTags": []
            |    }
            |  ]
            |}
        """.trimMargin("|"),
        )
    }
}
