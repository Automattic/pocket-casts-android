package au.com.shiftyjelly.pocketcasts.repositories.referrals

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import javax.inject.Inject

class ReferralManagerImpl @Inject constructor(
    var syncManager: SyncManager,
) : ReferralManager {

    override suspend fun getReferralCode(): ReferralCodeResponse {
        return syncManager.getReferralCode()
    }

    override suspend fun validateReferralCode(code: String): ReferralValidationResponse {
        return syncManager.validateReferralCode(code)
    }

    override suspend fun redeemReferralCode(code: String): ReferralRedemptionResponse {
        return syncManager.redeemReferralCode(code)
    }
}
