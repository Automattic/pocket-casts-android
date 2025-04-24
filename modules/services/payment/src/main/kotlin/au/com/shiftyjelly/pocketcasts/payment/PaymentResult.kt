package au.com.shiftyjelly.pocketcasts.payment

sealed interface PaymentResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : PaymentResult<T>

    data class Failure(val message: String) : PaymentResult<Nothing>
}

fun <T : Any> PaymentResult<T>.getOrNull() = when (this) {
    is PaymentResult.Success -> value
    is PaymentResult.Failure -> null
}
