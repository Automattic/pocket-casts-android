package au.com.shiftyjelly.pocketcasts.chat

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatPaywallViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val paymentDataSource = FakePaymentDataSource()

    @Test
    fun `show free trial when plus monthly trial is available`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().isFreeTrialAvailable)
        }
    }

    @Test
    fun `hide free trial when subscriptions fail to load`() = runTest {
        paymentDataSource.loadedProductsResultCode = PaymentResultCode.ServiceUnavailable
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitItem().isFreeTrialAvailable)
        }
    }

    private fun createViewModel() = ChatPaywallViewModel(
        paymentClient = PaymentClient.test(paymentDataSource),
    )
}
