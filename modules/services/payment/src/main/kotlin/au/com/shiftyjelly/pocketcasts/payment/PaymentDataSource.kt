package au.com.shiftyjelly.pocketcasts.payment

interface PaymentDataSource {
    suspend fun loadProducts(): PaymentResult<List<Product>>

    companion object {
        fun fake() = FakePaymentDataSource()
    }
}
