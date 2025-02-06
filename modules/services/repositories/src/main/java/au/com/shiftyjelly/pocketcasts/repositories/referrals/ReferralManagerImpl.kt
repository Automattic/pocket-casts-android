package au.com.shiftyjelly.pocketcasts.repositories.referrals

import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import javax.inject.Inject

class ReferralManagerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val networkWrapper: NetworkWrapper,
) : ReferralManager {

    override suspend fun getReferralCode() = try {
        if (!networkWrapper.isConnected()) throw NoNetworkException()
        ReferralResult.create(syncManager.getReferralCode())
    } catch (e: Exception) {
        ReferralResult.create(e)
    }

    override suspend fun getWinbackResponse() = try {
        ReferralResult.create(syncManager.getWinbackOffer())
    } catch (e: Exception) {
        ReferralResult.create(e)
    }

    override suspend fun validateReferralCode(code: String) = try {
        if (!networkWrapper.isConnected()) throw NoNetworkException()
        ReferralResult.create(syncManager.validateReferralCode(code))
    } catch (e: Exception) {
        ReferralResult.create(e)
    }

    override suspend fun redeemReferralCode(code: String) = try {
        if (!networkWrapper.isConnected()) throw NoNetworkException()
        ReferralResult.create(syncManager.redeemReferralCode(code))
    } catch (e: Exception) {
        ReferralResult.create(e)
    }
}
