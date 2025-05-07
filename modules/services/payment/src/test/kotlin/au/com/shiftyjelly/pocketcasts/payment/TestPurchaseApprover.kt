package au.com.shiftyjelly.pocketcasts.payment

class TestPurchaseApprover : PurchaseApprover {
    var approveResultCode: PaymentResultCode = PaymentResultCode.Ok

    var receivedPurchases = emptyList<Purchase>()
        private set

    override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
        receivedPurchases += purchase
        return if (approveResultCode == PaymentResultCode.Ok) {
            PaymentResult.Success(purchase)
        } else {
            PaymentResult.Failure(approveResultCode, "Error message")
        }
    }
}
