package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.extensions.isPositive
import au.com.shiftyjelly.pocketcasts.utils.extensions.price
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringBillingPeriod
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialBillingPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Period
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class UpgradeAccountViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val productDetails = subscriptionManager.observeProductDetails().map {
        if (it is ProductDetailsState.Loaded) {
            val product =
                it.productDetails.find { detail -> detail.productId == SubscriptionManager.TEST_FREE_TRIAL_SKU }
            val price = product?.price
            val isYearlyPlan = product?.recurringBillingPeriod?.years.isPositive()
            if (price != null) {
                Optional.of(
                    product.trialBillingPeriod?.let { trialBillingPeriod ->
                        ProductState.ProductWithTrial(
                            featureLabel = context.resources.getString(
                                LR.string.profile_feature_try_trial,
                                getFormattedTrialPeriod(trialBillingPeriod)
                            ),
                            price = context.resources.getString(
                                if (isYearlyPlan) LR.string.plus_per_year_with_trial else LR.string.plus_per_month_with_trial,
                                getFormattedTrialPeriod(trialBillingPeriod),
                                price
                            )
                        )
                    } ?: ProductState.ProductWithoutTrial(
                        featureLabel = context.resources.getString(LR.string.profile_feature_requires),
                        price = context.resources.getString(
                            if (isYearlyPlan) LR.string.plus_per_year else LR.string.plus_per_month,
                            price
                        )
                    )
                )
            } else {
                Optional.empty()
            }
        } else {
            Optional.empty()
        }
    }

    private fun getFormattedTrialPeriod(
        trialBillingPeriod: Period,
    ) = when {
        trialBillingPeriod.days > 0 -> context.resources.getStringPluralDays(trialBillingPeriod.days)
        trialBillingPeriod.months > 0 -> context.resources.getStringPluralMonths(trialBillingPeriod.months)
        trialBillingPeriod.years > 0 -> context.resources.getStringPluralYears(trialBillingPeriod.years)
        else -> context.resources.getStringPluralMonths(trialBillingPeriod.months)
    }

    val productState: LiveData<Optional<ProductState>> =
        LiveDataReactiveStreams.fromPublisher(productDetails)

    sealed class ProductState {
        abstract val featureLabel: String
        abstract val price: String
        abstract val buttonLabel: Int
        data class ProductWithTrial(
            override val featureLabel: String,
            override val price: String,
            @StringRes override val buttonLabel: Int = LR.string.profile_start_free_trial
        ) : ProductState()

        data class ProductWithoutTrial(
            override val featureLabel: String,
            override val price: String,
            @StringRes override val buttonLabel: Int = LR.string.profile_upgrade_to_plus
        ) : ProductState()
    }
}
