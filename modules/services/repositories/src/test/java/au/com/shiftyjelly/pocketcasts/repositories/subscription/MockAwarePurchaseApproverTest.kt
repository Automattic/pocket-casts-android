package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.mock.MockPaymentDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
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
    private val approver = MockAwarePurchaseApprover(serverApprover)

    @Test
    fun `approves mock purchases without calling the server`() = runTest {
        val mockPurchase = purchase.copy(token = MockPaymentDataSource.MOCK_PURCHASE_TOKEN)

        val result = approver.approve(mockPurchase)

        assertEquals(PaymentResult.Success(mockPurchase), result)
        verify(serverApprover, never()).approve(any())
    }

    @Test
    fun `sends real purchases to the server`() = runTest {
        val serverResult = PaymentResult.Failure(PaymentResultCode.ItemNotApproved, "Rejected")
        whenever(serverApprover.approve(purchase)).thenReturn(serverResult)

        val result = approver.approve(purchase)

        assertEquals(serverResult, result)
    }
}
