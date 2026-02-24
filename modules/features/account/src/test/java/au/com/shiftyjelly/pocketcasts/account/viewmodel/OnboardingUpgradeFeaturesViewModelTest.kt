package au.com.shiftyjelly.pocketcasts.account.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingUpgradeFeaturesViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val paymentDataSource = FakePaymentDataSource()
    private val paymentClient = PaymentClient.test(paymentDataSource)
    private val analyticsTracker = mock<AnalyticsTracker>()
    private val notificationManager = mock<NotificationManager>()
    private val experimentProvider = mock<ExperimentProvider>()
    private val flow = OnboardingFlow.InitialOnboarding

    @Test
    fun `given feature flag off, when subscriptions load, then shouldUseInstallmentPlans is false`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, false)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Treatment())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            assertFalse("Should not use installment plans when feature flag is disabled", state?.shouldUseInstallmentPlans ?: true)
        }
    }

    @Test
    fun `given feature flag on and control variation, when subscriptions load, then shouldUseInstallmentPlans is false`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Control)

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            assertFalse("Should not use installment plans for control group", state?.shouldUseInstallmentPlans ?: true)
        }
    }

    @Test
    fun `given feature flag on and treatment variation, when subscriptions load, then shouldUseInstallmentPlans is true`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Treatment())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            assertTrue("Should use installment plans for treatment group", state?.shouldUseInstallmentPlans ?: false)
        }
    }

    @Test
    fun `given control variation, when plus yearly plan selected, then regular plan is used`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Control)

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            val selectedPlan = state?.availablePlans?.firstOrNull {
                it.key.tier == SubscriptionTier.Plus && it.key.billingCycle == BillingCycle.Yearly
            }
            assertFalse("Plus yearly plan should not be installment in control group", selectedPlan?.key?.isInstallment ?: true)
        }
    }

    @Test
    fun `given treatment variation, when plus yearly plan selected, then installment plan is used if available`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Treatment())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            val selectedPlan = state?.availablePlans?.firstOrNull {
                it.key.tier == SubscriptionTier.Plus && it.key.billingCycle == BillingCycle.Yearly
            }
            // Should be installment if available in the data source (it is in FakePaymentDataSource by default)
            assertTrue("Plus yearly plan should be installment in treatment group", selectedPlan?.key?.isInstallment ?: false)
        }
    }

    @Test
    fun `given treatment variation but installments not available, when subscriptions load, then regular plan is used as fallback`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Treatment())

        // Remove installment products
        paymentDataSource.loadedProducts = paymentDataSource.loadedProducts.filterNot {
            it.id.contains("installment")
        }

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            val selectedPlan = state?.availablePlans?.firstOrNull {
                it.key.tier == SubscriptionTier.Plus && it.key.billingCycle == BillingCycle.Yearly
            }
            // Should fallback to regular plan when installment not available
            assertFalse("Should fallback to regular plan when installments unavailable", selectedPlan?.key?.isInstallment ?: true)
        }
    }

    @Test
    fun `given subscriptions fail to load, when vm init, then state is NoSubscriptions`() = runTest {
        paymentDataSource.loadedProducts = emptyList()
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Control)

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue("State should be NoSubscriptions when loading fails", state is OnboardingUpgradeFeaturesState.NoSubscriptions)
        }
    }

    @Test
    fun `given treatment variation, when change billing cycle, then installment state persists`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_INSTALLMENT_PLAN, true)
        whenever(experimentProvider.getVariation(Experiment.YearlyInstallments)).thenReturn(Variation.Treatment())

        val viewModel = createViewModel()

        viewModel.state.test {
            val initialState = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            assertEquals(BillingCycle.Yearly, initialState?.selectedBillingCycle)
            assertTrue("Should use installments initially", initialState?.shouldUseInstallmentPlans ?: false)

            // Change to monthly
            viewModel.changeBillingCycle(BillingCycle.Monthly)

            val updatedState = awaitItem() as? OnboardingUpgradeFeaturesState.Loaded
            assertEquals(BillingCycle.Monthly, updatedState?.selectedBillingCycle)
            assertTrue("Should still have installment flag after billing cycle change", updatedState?.shouldUseInstallmentPlans ?: false)
        }
    }

    private fun createViewModel(): OnboardingUpgradeFeaturesViewModel {
        return OnboardingUpgradeFeaturesViewModel(
            paymentClient = paymentClient,
            analyticsTracker = analyticsTracker,
            notificationManager = notificationManager,
            experimentProvider = experimentProvider,
            flow = flow,
        )
    }
}
