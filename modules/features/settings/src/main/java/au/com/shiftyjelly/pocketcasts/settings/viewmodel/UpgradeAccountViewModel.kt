package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.util.BillingPeriodHelper
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.extensions.isPositive
import au.com.shiftyjelly.pocketcasts.utils.extensions.price
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringBillingPeriod
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialBillingPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class UpgradeAccountViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager,
    billingPeriodHelper: BillingPeriodHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val productDetails = subscriptionManager.observeProductDetails().map {
        if (it is ProductDetailsState.Loaded) {
            val product =
                it.productDetails.find { detail -> detail.productId == SubscriptionManager.TEST_FREE_TRIAL_PRODUCT_ID }
            val price = product?.price
            val isYearlyPlan = product?.recurringBillingPeriod?.years.isPositive()
            if (price != null) {
                Optional.of(
                    product.trialBillingPeriod?.let { trialBillingPeriod ->
                        val billingDetails = billingPeriodHelper.mapToBillingDetails(trialBillingPeriod)
                        ProductState.ProductWithTrial(
                            featureLabelLine2 = context.resources.getString(
                                LR.string.profile_feature_try_trial_billing_info,
                                billingDetails.periodValue
                            ),
                            price = context.resources.getString(
                                if (isYearlyPlan) LR.string.plus_per_year_with_trial else LR.string.plus_per_month_with_trial,
                                billingDetails.periodValue,
                                price
                            )
                        )
                    } ?: ProductState.ProductWithoutTrial(
                        featureLabelLine2 = context.resources.getString(LR.string.pocket_casts_plus),
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

    val productState: LiveData<Optional<ProductState>> =
        LiveDataReactiveStreams.fromPublisher(productDetails)

    sealed class ProductState {
        abstract val featureLabelLine1: Int
        abstract val featureLabelLine2: String
        abstract val price: String
        abstract val buttonLabel: Int
        data class ProductWithTrial(
            @StringRes override val featureLabelLine1: Int = LR.string.profile_feature_try_trial,
            override val featureLabelLine2: String,
            override val price: String,
            @StringRes override val buttonLabel: Int = LR.string.profile_start_free_trial
        ) : ProductState()

        data class ProductWithoutTrial(
            @StringRes override val featureLabelLine1: Int = LR.string.profile_feature_requires,
            override val featureLabelLine2: String,
            override val price: String,
            @StringRes override val buttonLabel: Int = LR.string.profile_upgrade_to_plus
        ) : ProductState()
    }
}
