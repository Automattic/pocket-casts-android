package au.com.shiftyjelly.pocketcasts.payment

sealed interface PaymentResult<out T : Any> {
    data class Success<out T : Any>(val value: T) : PaymentResult<T>

    data class Failure(val message: String) : PaymentResult<Nothing>
}

fun <T : Any, R : Any> PaymentResult<T>.map(block: (T) -> R) = when (this) {
    is PaymentResult.Success -> PaymentResult.Success(block(value))
    is PaymentResult.Failure -> this
}

fun <T : Any, R : Any> PaymentResult<T>.flatMap(block: (T) -> PaymentResult<R>) = when (this) {
    is PaymentResult.Success -> block(value)
    is PaymentResult.Failure -> this
}

fun <T : Any> PaymentResult<T>.onSuccess(block: (T) -> Unit) = apply {
    if (this is PaymentResult.Success) {
        block(value)
    }
}

fun <T : Any> PaymentResult<T>.onFailure(block: (String) -> Unit) = apply {
    if (this is PaymentResult.Failure) {
        block(message)
    }
}

fun <T : Any> PaymentResult<T>.getOrNull() = when (this) {
    is PaymentResult.Success -> value
    is PaymentResult.Failure -> null
}
