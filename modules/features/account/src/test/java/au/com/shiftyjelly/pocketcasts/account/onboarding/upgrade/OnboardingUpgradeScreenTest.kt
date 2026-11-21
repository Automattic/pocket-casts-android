package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingUpgradeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `intro offer shows the holiday tag, headline, button and footnote`() {
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        setScreen(billingCycle = BillingCycle.Yearly, source = OnboardingUpgradeSource.ACCOUNT_DETAILS)

        composeTestRule.onNodeWithText("Limited time offer").assertExists()
        composeTestRule.onNodeWithText("50% off Plus for\none year").assertExists()
        composeTestRule.onNodeWithText("Claim 50% off").assertExists()
        composeTestRule.onNodeWithText("Recurring payments at", substring = true).assertExists()
        composeTestRule.onNodeWithText("after 1 year", substring = true).assertExists()
        composeTestRule.onNodeWithText("Terms\u00A0of\u00A0Use", substring = true).assertExists()
    }

    @Test
    fun `selecting monthly during the intro offer restores the regular header, button and footer`() {
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        setScreen(billingCycle = BillingCycle.Monthly, source = OnboardingUpgradeSource.ACCOUNT_DETAILS)

        composeTestRule.onNodeWithText("Limited time offer").assertDoesNotExist()
        composeTestRule.onNodeWithText("50% off Plus for\none year").assertDoesNotExist()
        composeTestRule.onNodeWithText("Superpowers for your podcasts").assertExists()
        composeTestRule.onNodeWithText("Subscribe to Plus").assertExists()
        composeTestRule.onNodeWithText("Claim", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Recurring payments at", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Terms\u00A0and\u00A0Conditions", substring = true).assertExists()
    }

    @Test
    fun `feature paywalls keep their own headline during the intro offer`() {
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        setScreen(billingCycle = BillingCycle.Yearly, source = OnboardingUpgradeSource.FOLDERS)

        composeTestRule.onNodeWithText("Limited time offer").assertExists()
        composeTestRule.onNodeWithText("off Plus for", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Claim 50% off").assertExists()
    }

    @Test
    fun `without the intro offer the paywall is unchanged`() {
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, false)

        setScreen(billingCycle = BillingCycle.Yearly, source = OnboardingUpgradeSource.ACCOUNT_DETAILS)

        composeTestRule.onNodeWithText("Limited time offer").assertDoesNotExist()
        composeTestRule.onNodeWithText("Superpowers for your podcasts").assertExists()
        composeTestRule.onNodeWithText("Claim", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("By continuing", substring = true).assertExists()
    }

    private fun setScreen(billingCycle: BillingCycle, source: OnboardingUpgradeSource) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            // The feature list includes a TextView that needs the app's XML theme
            val themedContext = ContextThemeWrapper(LocalContext.current, Theme.ThemeType.LIGHT.resourceId)
            CompositionLocalProvider(LocalContext provides themedContext) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    OnboardingUpgradeScreen(
                        state = OnboardingUpgradeFeaturesState.Loaded(
                            subscriptionPlans = SubscriptionPlans.Preview,
                            selectedTier = SubscriptionTier.Plus,
                            selectedBillingCycle = billingCycle,
                            plansFilter = OnboardingUpgradeFeaturesState.LoadedPlansFilter.PLUS_ONLY,
                            purchaseFailed = false,
                        ),
                        source = source,
                        onClosePress = {},
                        onSubscribePress = {},
                        onChangeSelectedPlan = {},
                        onClickPrivacyPolicy = {},
                        onClickTermsAndConditions = {},
                        onClickSeeAllFeatures = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
    }
}
