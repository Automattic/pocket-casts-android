package au.com.shiftyjelly.pocketcasts.repositories.referrals

import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.ErrorResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.SuccessResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

class ReferralManagerImplTest {

    private var syncManager: SyncManager = mock()
    private var networkWrapper: NetworkWrapper = mock()
    private lateinit var referralManager: ReferralManagerImpl

    @Before
    fun setUp() {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        referralManager = ReferralManagerImpl(
            syncManager,
            networkWrapper,
        )
    }

    @Test
    fun `given no error, when referral code is called, then returns success code result`() = runTest {
        val httpResponse = mockHttpResponse(true, ReferralCodeResponse::class.java)
        whenever(syncManager.getReferralCode()).thenReturn(httpResponse)

        val result = referralManager.getReferralCode()

        assertTrue(result is SuccessResult<ReferralCodeResponse>)
    }

    @Test
    fun `given unsuccessful response, when referral code is called, then returns error result`() = runTest {
        val httpResponse = mockHttpResponse(false, ReferralCodeResponse::class.java)
        whenever(syncManager.getReferralCode()).thenReturn(httpResponse)

        val result = referralManager.getReferralCode()

        assertTrue(result is ErrorResult<ReferralCodeResponse>)
    }

    @Test
    fun `given no network, when referral code is called, then returns error result with NoNetworkException`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(false)
        referralManager = ReferralManagerImpl(syncManager, networkWrapper)

        val result = referralManager.getReferralCode()

        assertTrue((result as ErrorResult).error is NoNetworkException)
    }

    @Test
    fun `given valid code, when referral code is validated, then returns validation success result`() = runTest {
        val code = "valid_code"
        val httpResponse = mockHttpResponse(true, ReferralValidationResponse::class.java)
        whenever(syncManager.validateReferralCode(code)).thenReturn(httpResponse)

        val result = referralManager.validateReferralCode(code)

        assertTrue(result is SuccessResult<ReferralValidationResponse>)
    }

    @Test
    fun `given invalid code, when referral code is validated, then error result`() = runTest {
        val code = "invalid_code"
        val httpResponse = mockHttpResponse(false, ReferralValidationResponse::class.java)
        whenever(syncManager.validateReferralCode(code)).thenReturn(httpResponse)

        val result = referralManager.validateReferralCode(code)

        assertTrue(result is ErrorResult<ReferralValidationResponse>)
    }

    @Test
    fun `given no network, when referral code is validated, then returns error result with NoNetworkException`() = runTest {
        val code = "valid_code"
        whenever(networkWrapper.isConnected()).thenReturn(false)
        referralManager = ReferralManagerImpl(syncManager, networkWrapper)

        val result = referralManager.validateReferralCode(code)

        assertTrue((result as ErrorResult).error is NoNetworkException)
    }

    @Test
    fun `given valid code, when referral code is redeemed, then returns redemption success result`() = runTest {
        val code = "valid_code"
        val httpResponse = mockHttpResponse(true, ReferralRedemptionResponse::class.java)
        whenever(syncManager.redeemReferralCode(code)).thenReturn(httpResponse)

        val result = referralManager.redeemReferralCode(code)

        assertTrue(result is SuccessResult<ReferralRedemptionResponse>)
    }

    @Test
    fun `given invalid code, when referral code is redeemed, then error result`() = runTest {
        val code = "invalid_code"
        val httpResponse = mockHttpResponse(false, ReferralRedemptionResponse::class.java)
        whenever(syncManager.redeemReferralCode(code)).thenReturn(httpResponse)

        val result = referralManager.redeemReferralCode(code)

        assertTrue(result is ErrorResult<ReferralRedemptionResponse>)
    }

    @Test
    fun `given no network, when referral code is redeemed, then returns error result with NoNetworkException`() = runTest {
        val code = "valid_code"
        whenever(networkWrapper.isConnected()).thenReturn(false)
        referralManager = ReferralManagerImpl(syncManager, networkWrapper)

        val result = referralManager.redeemReferralCode(code)

        assertTrue((result as ErrorResult).error is NoNetworkException)
    }

    private fun <T> mockHttpResponse(success: Boolean, responseClass: Class<T>): Response<T> {
        val httpResponse = mock<Response<T>>()
        whenever(httpResponse.isSuccessful).thenReturn(success)
        if (success) {
            val expectedResponse = mock(responseClass)
            whenever(httpResponse.body()).thenReturn(expectedResponse)
        } else {
            val expectedResponse = mock<ResponseBody>()
            whenever(httpResponse.errorBody()).thenReturn(expectedResponse)
        }
        return httpResponse
    }
}
