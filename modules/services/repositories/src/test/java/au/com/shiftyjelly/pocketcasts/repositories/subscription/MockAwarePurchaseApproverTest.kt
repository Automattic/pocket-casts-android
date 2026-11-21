package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.mock.MockBillingSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MockAwarePurchaseApproverTest {
    private val purchase = Purchase(
        state = PurchaseState.Purchased("order-id"),
        token = "token",
        productIds = listOf("product-id"),
        isAcknowledged = false,
        isAutoRenewing = true,
    )
    private val serverApprover = mock<ServerPurchaseApprover>()

    @Test
    fun `approves mock purchases without calling the server`() = runTest {
        val approver = MockAwarePurchaseApprover(serverApprover, mockSettings(isMockEnabled = true))

        val result = approver.approve(purchase)

        assertEquals(PaymentResult.Success(purchase), result)
        verify(serverApprover, never()).approve(any())
    }

    @Test
    fun `sends real purchases to the server`() = runTest {
        val serverResult = PaymentResult.Failure(PaymentResultCode.ItemNotApproved, "Rejected")
        whenever(serverApprover.approve(purchase)).thenReturn(serverResult)
        val approver = MockAwarePurchaseApprover(serverApprover, mockSettings(isMockEnabled = false))

        val result = approver.approve(purchase)

        assertEquals(serverResult, result)
    }

    private fun mockSettings(isMockEnabled: Boolean) = mock<MockBillingSettings> {
        on { this.isMockEnabled } doReturn isMockEnabled
    }
}
