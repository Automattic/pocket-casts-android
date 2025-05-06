package au.com.shiftyjelly.pocketcasts.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class TestPurchaseApprover : PurchaseApprover {
    var receivedPurchases = emptyList<Purchase>()
        private set

    private val approveFlow = MutableSharedFlow<PaymentResultCode>()

    suspend fun emitApproveResponse(code: PaymentResultCode) {
        approveFlow.emit(code)
    }

    override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
        receivedPurchases += purchase
        val errorCode = approveFlow.first()
        return if (errorCode == PaymentResultCode.Ok) {
            PaymentResult.Success(purchase)
        } else {
            PaymentResult.Failure(errorCode, "Error")
        }
    }
}
