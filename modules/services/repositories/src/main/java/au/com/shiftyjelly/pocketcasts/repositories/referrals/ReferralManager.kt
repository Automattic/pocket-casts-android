package au.com.shiftyjelly.pocketcasts.repositories.referrals

import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import com.pocketcasts.service.api.WinbackResponse
import retrofit2.Response

interface ReferralManager {
    suspend fun getReferralCode(): ReferralResult<ReferralCodeResponse>
    suspend fun getWinbackResponse(): ReferralResult<WinbackResponse>
    suspend fun validateReferralCode(code: String): ReferralResult<ReferralValidationResponse>
    suspend fun redeemReferralCode(code: String): ReferralResult<ReferralRedemptionResponse>

    sealed class ReferralResult<T> {
        companion object {
            fun <T> create(error: Throwable): ErrorResult<T> {
                return ErrorResult(error.message ?: "unknown error", error = error)
            }

            fun <T> create(response: Response<T>): ReferralResult<T> {
                return if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null || response.code() == 204) {
                        EmptyResult()
                    } else {
                        SuccessResult(body = body)
                    }
                } else {
                    val msg = response.errorBody()?.string()
                    val errorMsg = if (msg.isNullOrEmpty()) {
                        response.message()
                    } else {
                        msg
                    }
                    ErrorResult(errorMsg ?: "unknown error")
                }
            }
        }

        class EmptyResult<T> : ReferralResult<T>()
        data class SuccessResult<T>(val body: T) : ReferralResult<T>()
        data class ErrorResult<T>(val errorMessage: String, val error: Throwable? = null) : ReferralResult<T>()
    }
}
