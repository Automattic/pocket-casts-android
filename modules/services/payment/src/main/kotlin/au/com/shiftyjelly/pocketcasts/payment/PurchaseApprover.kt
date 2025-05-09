package au.com.shiftyjelly.pocketcasts.payment

interface PurchaseApprover {
    suspend fun approve(purchase: Purchase): PaymentResult<Purchase>
}
