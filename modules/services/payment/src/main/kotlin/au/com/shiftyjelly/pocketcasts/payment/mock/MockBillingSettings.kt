package au.com.shiftyjelly.pocketcasts.payment.mock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.payment.BuildConfig
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class MockBillingSettings internal constructor(
    private val preferences: SharedPreferences,
    private val isDeveloperBuild: Boolean,
    private val canUseRealBilling: Boolean,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        isDeveloperBuild = BuildConfig.DEBUG || BuildConfig.IS_PROTOTYPE,
        canUseRealBilling = context.packageName == PLAY_STORE_PACKAGE_NAME,
    )

    // Google Play only sells subscriptions to the Play Store package, so any other package always uses mocks
    val canToggleMock = isDeveloperBuild && canUseRealBilling

    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<MockBillingConfig> = _config.asStateFlow()

    val isMockEnabled get() = config.value.isEnabled

    fun setMockEnabled(isEnabled: Boolean) {
        if (!canToggleMock) return
        preferences.edit { putBoolean(KEY_MOCK_ENABLED, isEnabled) }
        _config.value = readConfig()
    }

    fun setOfferEligible(offer: SubscriptionOffer, isEligible: Boolean) {
        val eligibleOffers = if (isEligible) {
            config.value.eligibleOffers + offer
        } else {
            config.value.eligibleOffers - offer
        }
        preferences.edit { putStringSet(KEY_ELIGIBLE_OFFERS, eligibleOffers.mapTo(mutableSetOf()) { it.name }) }
        _config.value = readConfig()
    }

    fun setFailProductsLoading(shouldFail: Boolean) {
        preferences.edit { putBoolean(KEY_FAIL_PRODUCTS_LOADING, shouldFail) }
        _config.value = readConfig()
    }

    fun setPurchaseOutcome(outcome: MockPurchaseOutcome) {
        preferences.edit { putString(KEY_PURCHASE_OUTCOME, outcome.name) }
        _config.value = readConfig()
    }

    private fun readConfig(): MockBillingConfig {
        val defaults = MockBillingConfig()
        val storedOffers = preferences.getStringSet(KEY_ELIGIBLE_OFFERS, null)
        return MockBillingConfig(
            isEnabled = !canUseRealBilling || (isDeveloperBuild && preferences.getBoolean(KEY_MOCK_ENABLED, false)),
            eligibleOffers = storedOffers
                ?.mapNotNullTo(mutableSetOf()) { name -> SubscriptionOffer.entries.find { it.name == name } }
                ?: defaults.eligibleOffers,
            failProductsLoading = preferences.getBoolean(KEY_FAIL_PRODUCTS_LOADING, defaults.failProductsLoading),
            purchaseOutcome = preferences.getString(KEY_PURCHASE_OUTCOME, null)
                ?.let { name -> MockPurchaseOutcome.entries.find { it.name == name } }
                ?: defaults.purchaseOutcome,
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "mock_google_play_billing"
        const val PLAY_STORE_PACKAGE_NAME = "au.com.shiftyjelly.pocketcasts"
        const val KEY_MOCK_ENABLED = "mock_enabled"
        const val KEY_ELIGIBLE_OFFERS = "eligible_offers"
        const val KEY_FAIL_PRODUCTS_LOADING = "fail_products_loading"
        const val KEY_PURCHASE_OUTCOME = "purchase_outcome"
    }
}
