package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpgradePlanRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val introOfferPlan = SubscriptionPlans.Preview
        .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer)
        .getOrNull()!!
    private val introPrice = introOfferPlan.pricingPhases.first().price.formattedPrice
    private val regularPrice = introOfferPlan.pricingPhases.last().price.formattedPrice

    @Test
    fun `intro offer shows the first year price, regular price and badge`() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                UpgradePlanRow(
                    plan = introOfferPlan,
                    isSelected = true,
                    onClick = {},
                    priceComparisonPlan = SubscriptionPlan.PlusMonthlyPreview,
                )
            }
        }

        composeTestRule.onNodeWithText("50% off").assertExists()
        composeTestRule.onNodeWithContentDescription("$introPrice first year, Regular price $regularPrice").assertExists()
        composeTestRule.onNodeWithText("Save", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("/week", substring = true).assertDoesNotExist()
    }

    @Test
    fun `base yearly plan keeps the savings badge and weekly price`() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                UpgradePlanRow(
                    plan = SubscriptionPlan.PlusYearlyPreview,
                    isSelected = true,
                    onClick = {},
                    priceComparisonPlan = SubscriptionPlan.PlusMonthlyPreview,
                )
            }
        }

        composeTestRule.onNodeWithText("Save", substring = true).assertExists()
        composeTestRule.onNodeWithText("/week", substring = true).assertExists()
        composeTestRule.onNodeWithText("first year", substring = true).assertDoesNotExist()
    }
}
