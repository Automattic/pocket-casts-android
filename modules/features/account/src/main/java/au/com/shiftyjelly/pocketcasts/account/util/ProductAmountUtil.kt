package au.com.shiftyjelly.pocketcasts.account.util

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.settings.util.BillingPeriodHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.SubscriptionBillingUnit
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringBillingPeriod
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringPrice
import au.com.shiftyjelly.pocketcasts.utils.extensions.toSubscriptionBillingUnit
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialBillingPeriod
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class ProductAmount(
    val primaryText: String,
    val secondaryText: String? = null
)

class ProductAmountUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingPeriodHelper: BillingPeriodHelper
) {
    fun get(productDetails: ProductDetails): ProductAmount {
        val trialBillingPeriod = productDetails.trialBillingPeriod
        val billingUnit = productDetails.recurringBillingPeriod?.toSubscriptionBillingUnit()
        val billingUnitStringResources = billingUnit?.toStringResources()

        return if (trialBillingPeriod == null) {
            val recurringPrice = productDetails.recurringPrice ?: ""
            val recurringBillingUnit = billingUnitStringResources?.let { (billingUnitStringRes, _) ->
                context.getString(billingUnitStringRes)
            } ?: ""

            ProductAmount(primaryText = "$recurringPrice / $recurringBillingUnit")
        } else {
            val billingDetails = billingPeriodHelper.mapToBillingDetails(trialBillingPeriod)
            val primaryText =
                context.getString(R.string.profile_amount_free, billingDetails.periodValue ?: "")
            val secondaryText =
                billingUnitStringResources?.let { (_, pricePerBillingUnitStringRes) ->
                    context.getString(pricePerBillingUnitStringRes, productDetails.recurringPrice)
                }

            ProductAmount(
                primaryText = primaryText,
                secondaryText = secondaryText
            )
        }
    }

    private fun SubscriptionBillingUnit.toStringResources(): Pair<Int, Int>? {
        return when (this) {
            SubscriptionBillingUnit.MONTHS -> Pair(R.string.plus_month, R.string.plus_per_month_then)
            SubscriptionBillingUnit.YEARS -> Pair(R.string.plus_year, R.string.plus_per_year_then)
            else -> {
                LogBuffer.e(
                    LogBuffer.TAG_SUBSCRIPTIONS,
                    "unexpected recurring billing frequency: $this"
                )
                null
            }
        }
    }
}
