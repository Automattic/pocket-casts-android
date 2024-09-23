package au.com.shiftyjelly.pocketcasts.repositories.referrals

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class ReferralManagerImplTest {

    private lateinit var syncManager: SyncManager
    private lateinit var referralManager: ReferralManagerImpl

    @Before
    fun setUp() {
        syncManager = mock(SyncManager::class.java)
        referralManager = ReferralManagerImpl(syncManager)
    }

    @Test
    fun `given no error, when referrals code is called, then returns referral code response`() = runTest {
        val expectedResponse = mock<ReferralCodeResponse>()
        whenever(syncManager.getReferralCode()).thenReturn(expectedResponse)

        val result = referralManager.getReferralCode()

        assertEquals(expectedResponse, result)
    }

    @Test(expected = Exception::class)
    fun `given server error, when referrals code is called, then throws exception`() = runTest {
        whenever(syncManager.getReferralCode()).thenThrow(Exception::class.java)

        referralManager.getReferralCode()
    }

    @Test
    fun `given valid code, when referral code is validated, then returns validation response`() = runTest {
        val code = "valid_code"
        val expectedResponse = mock<ReferralValidationResponse>()
        whenever(syncManager.validateReferralCode(code)).thenReturn(expectedResponse)

        val result = referralManager.validateReferralCode(code)

        assertEquals(expectedResponse, result)
    }

    @Test(expected = Exception::class)
    fun `given invalid code, when referral code is validated, then throws exception`() = runTest {
        val code = "invalid_code"
        whenever(syncManager.validateReferralCode(code)).thenThrow(Exception::class.java)

        referralManager.validateReferralCode(code)
    }

    @Test
    fun `given valid code, when referral code is redeemed, then returns redemption response`() = runTest {
        val code = "valid_code"
        val expectedResponse = mock<ReferralRedemptionResponse>()
        whenever(syncManager.redeemReferralCode(code)).thenReturn(expectedResponse)

        val result = referralManager.redeemReferralCode(code)

        assertEquals(expectedResponse, result)
    }

    @Test(expected = Exception::class)
    fun `given invalid code, when referral code is redeemed, then throws exception`() = runTest {
        val code = "invalid_code"
        whenever(syncManager.redeemReferralCode(code)).thenThrow(Exception::class.java)

        referralManager.redeemReferralCode(code)
    }
}