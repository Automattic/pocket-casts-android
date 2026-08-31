package au.com.shiftyjelly.pocketcasts.utils.fingerprint

import org.junit.Assert.assertEquals
import org.junit.Test

class FingerprintDecodePolicyTest {
    @Test
    fun `platform for non-huawei device`() {
        assertEquals(FingerprintPolicy.PLATFORM, resolvePixel(sdkInt = 28, override = ""))
    }

    @Test
    fun `platform for huawei on modern android`() {
        assertEquals(FingerprintPolicy.PLATFORM, resolveHuaweiKirin(sdkInt = 29, override = ""))
    }

    @Test
    fun `platform for huawei without a hisilicon soc`() {
        val policy = FingerprintDecodePolicy.resolve(
            sdkInt = 28,
            manufacturer = "HUAWEI",
            brand = "HUAWEI",
            hardware = "qcom",
            board = "msm8998",
            override = "",
        )
        assertEquals(FingerprintPolicy.PLATFORM, policy)
    }

    @Test
    fun `tap only for huawei hisilicon on legacy android`() {
        assertEquals(FingerprintPolicy.TAP_ONLY, resolveHuaweiKirin(sdkInt = 28, override = ""))
    }

    @Test
    fun `tap only for honor hisilicon`() {
        val policy = FingerprintDecodePolicy.resolve(
            sdkInt = 27,
            manufacturer = "HONOR",
            brand = "HONOR",
            hardware = "hi3660",
            board = "hi3660",
            override = "",
        )
        assertEquals(FingerprintPolicy.TAP_ONLY, policy)
    }

    @Test
    fun `garbage override falls back to tap only on affected devices`() {
        assertEquals(FingerprintPolicy.TAP_ONLY, resolveHuaweiKirin(sdkInt = 28, override = "banana"))
    }

    @Test
    fun `override disables the subsystem on affected devices`() {
        assertEquals(FingerprintPolicy.DISABLED, resolveHuaweiKirin(sdkInt = 28, override = "disabled"))
    }

    @Test
    fun `override can restore the platform path on affected devices`() {
        assertEquals(FingerprintPolicy.PLATFORM, resolveHuaweiKirin(sdkInt = 28, override = "PLATFORM"))
    }

    @Test
    fun `override is ignored on non-affected devices`() {
        assertEquals(FingerprintPolicy.PLATFORM, resolvePixel(sdkInt = 28, override = "DISABLED"))
    }

    private fun resolveHuaweiKirin(sdkInt: Int, override: String) = FingerprintDecodePolicy.resolve(
        sdkInt = sdkInt,
        manufacturer = "HUAWEI",
        brand = "HUAWEI",
        hardware = "kirin970",
        board = "kirin970",
        override = override,
    )

    private fun resolvePixel(sdkInt: Int, override: String) = FingerprintDecodePolicy.resolve(
        sdkInt = sdkInt,
        manufacturer = "Google",
        brand = "google",
        hardware = "walleye",
        board = "walleye",
        override = override,
    )
}
