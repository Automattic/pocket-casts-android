package au.com.shiftyjelly.pocketcasts.payment

sealed interface PaymentResult<out T : Any> {
    data class Success<out T : Any>(
        val value: T,
    ) : PaymentResult<T>

    data class Failure constructor(
        val code: PaymentResultCode,
        val message: String,
    ) : PaymentResult<Nothing>
}

sealed interface PaymentResultCode {
    data object Error : PaymentResultCode
    data object FeatureNotSupported : PaymentResultCode
    data object ServiceDisconnected : PaymentResultCode
    data object Ok : PaymentResultCode
    data object UserCancelled : PaymentResultCode
    data object ServiceUnavailable : PaymentResultCode
    data object BillingUnavailable : PaymentResultCode
    data object ItemUnavailable : PaymentResultCode
    data object DeveloperError : PaymentResultCode
    data object ItemAlreadyOwned : PaymentResultCode
    data object ItemNotOwned : PaymentResultCode
    data object NetworkError : PaymentResultCode
    data class Unknown(val code: Int) : PaymentResultCode
}

inline fun <T : Any, R : Any> PaymentResult<T>.map(block: (T) -> R) = when (this) {
    is PaymentResult.Success -> PaymentResult.Success(block(value))
    is PaymentResult.Failure -> this
}

inline fun <T : Any, R : Any> PaymentResult<T>.flatMap(block: (T) -> PaymentResult<R>) = when (this) {
    is PaymentResult.Success -> block(value)
    is PaymentResult.Failure -> this
}

inline fun <T : Any> PaymentResult<T>.recover(block: (PaymentResultCode, String) -> PaymentResult<T>) = when (this) {
    is PaymentResult.Success -> this
    is PaymentResult.Failure -> block(code, message)
}

inline fun <T : Any> PaymentResult<T>.onSuccess(block: (T) -> Unit) = apply {
    if (this is PaymentResult.Success) {
        block(value)
    }
}

inline fun <T : Any> PaymentResult<T>.onFailure(block: (PaymentResultCode, String) -> Unit) = apply {
    if (this is PaymentResult.Failure) {
        block(code, message)
    }
}

fun <T : Any> PaymentResult<T>.getOrNull() = when (this) {
    is PaymentResult.Success -> value
    is PaymentResult.Failure -> null
}

sealed interface PurchaseResult {
    data object Purchased : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failure(val code: PaymentResultCode) : PurchaseResult
}
