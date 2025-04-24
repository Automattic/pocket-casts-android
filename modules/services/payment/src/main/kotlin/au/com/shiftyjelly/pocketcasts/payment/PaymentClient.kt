package au.com.shiftyjelly.pocketcasts.payment

class PaymentClient(
    private val dataSource: PaymentDataSource,
    private val logger: Logger,
) {
    suspend fun loadSubscriptionPlans(): PaymentResult<SubscriptionPlans> {
        logger.info("Load subscription plans")
        return dataSource
            .loadProducts()
            .flatMap(SubscriptionPlans::create)
            .onSuccess { plans -> logger.info("Subscription plans loaded: $plans") }
            .onFailure { message -> logger.warning("Failed to load subscription plans: $message") }
    }
}
