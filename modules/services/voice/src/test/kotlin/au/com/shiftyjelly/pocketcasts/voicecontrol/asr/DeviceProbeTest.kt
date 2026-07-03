package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProbeTest {

    @Test
    fun `isSnapdragon returns true when hardware contains qcom`() {
        val probe = DeviceProbe(hardware = "qcom", socManufacturer = "", sdkInt = 30)
        assertTrue(probe.isSnapdragon)
    }

    @Test
    fun `isSnapdragon returns true when soc_manufacturer is qualcomm`() {
        val probe = DeviceProbe(hardware = "rockchip", socManufacturer = "Qualcomm", sdkInt = 30)
        assertTrue(probe.isSnapdragon)
    }

    @Test
    fun `isSnapdragon returns true when soc_manufacturer contains qualcomm case-insensitively`() {
        val probe = DeviceProbe(hardware = "generic", socManufacturer = "QUALCOMM Technologies", sdkInt = 30)
        assertTrue(probe.isSnapdragon)
    }

    @Test
    fun `isSnapdragon returns false for non-snapdragon hardware`() {
        val probe = DeviceProbe(hardware = "exynos", socManufacturer = "Samsung", sdkInt = 30)
        assertFalse(probe.isSnapdragon)
    }

    @Test
    fun `isSnapdragon returns false when both hardware and soc_manufacturer are empty`() {
        val probe = DeviceProbe(hardware = "", socManufacturer = "", sdkInt = 30)
        assertFalse(probe.isSnapdragon)
    }

    @Test
    fun `hasNpu returns false`() {
        val probe = DeviceProbe(hardware = "", socManufacturer = "", sdkInt = 30)
        assertFalse(probe.hasNpu)
    }

    @Test
    fun `apiLevel matches constructor arg`() {
        val probe = DeviceProbe(hardware = "", socManufacturer = "", sdkInt = 33)
        assertEquals(33, probe.apiLevel)
    }

    @Test
    fun `hasSufficientRam returns true for API 26 and above`() {
        val probe = DeviceProbe(hardware = "", socManufacturer = "", sdkInt = 26)
        assertTrue(probe.hasSufficientRam)
    }

    @Test
    fun `hasSufficientRam returns false for API below 26`() {
        val probe = DeviceProbe(hardware = "", socManufacturer = "", sdkInt = 25)
        assertFalse(probe.hasSufficientRam)
    }
}
