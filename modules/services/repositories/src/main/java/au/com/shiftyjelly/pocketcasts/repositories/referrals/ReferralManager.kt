package au.com.shiftyjelly.pocketcasts.repositories.referrals

import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse

interface ReferralManager {
    suspend fun getReferralCode(): ReferralCodeResponse
    suspend fun validateReferralCode(code: String): ReferralValidationResponse
    suspend fun redeemReferralCode(code: String): ReferralRedemptionResponse
}
