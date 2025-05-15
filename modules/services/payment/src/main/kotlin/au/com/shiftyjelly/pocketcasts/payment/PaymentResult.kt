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
    val analyticsValue: String

    data object Error : PaymentResultCode {
        override val analyticsValue get() = "error"
    }

    data object FeatureNotSupported : PaymentResultCode {
        override val analyticsValue get() = "feature_not_suported"
    }

    data object ServiceDisconnected : PaymentResultCode {
        override val analyticsValue get() = "service_disconnected"
    }

    data object Ok : PaymentResultCode {
        override val analyticsValue get() = "ok"
    }

    data object UserCancelled : PaymentResultCode {
        override val analyticsValue get() = "user_cancelled"
    }

    data object ServiceUnavailable : PaymentResultCode {
        override val analyticsValue get() = "service_unavailable"
    }

    data object BillingUnavailable : PaymentResultCode {
        override val analyticsValue get() = "billing_unavailable"
    }

    data object ItemUnavailable : PaymentResultCode {
        override val analyticsValue get() = "item_unavailable"
    }

    data object ItemNotApproved : PaymentResultCode {
        override val analyticsValue get() = "item_not_approved"
    }

    data object DeveloperError : PaymentResultCode {
        override val analyticsValue get() = "developer_error"
    }

    data object ItemAlreadyOwned : PaymentResultCode {
        override val analyticsValue get() = "item_already_owned"
    }

    data object ItemNotOwned : PaymentResultCode {
        override val analyticsValue get() = "item_not_owned"
    }

    data object NetworkError : PaymentResultCode {
        override val analyticsValue get() = "network_error"
    }

    data class Unknown(val code: Int) : PaymentResultCode {
        override val analyticsValue get() = "unknown"
    }
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
