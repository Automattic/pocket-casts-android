package au.com.shiftyjelly.pocketcasts.views.helper

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BatteryWarningPolicyTest {
    private val settings = mock<Settings>()
    private val systemBatteryRestrictions = mock<SystemBatteryRestrictions>()

    private val policy = newAppOpen()

    @Before
    fun setUp() {
        whenever(systemBatteryRestrictions.isUnrestricted()) doReturn false
        whenever(settings.getTimesToShowBatteryWarning()) doReturn 3
    }

    @Test
    fun `a warning is shown when the battery is restricted`() {
        assertTrue(policy.shouldShowWarning())
    }

    @Test
    fun `showing a warning uses one of the remaining warnings`() {
        policy.onWarningShown()

        verify(settings).setTimesToShowBatteryWarning(2)
    }

    @Test
    fun `only one warning is shown per app open`() {
        policy.onWarningShown()

        assertFalse(policy.shouldShowWarning())
    }

    @Test
    fun `reopening the app allows another warning`() {
        policy.onWarningShown()

        assertTrue(newAppOpen().shouldShowWarning())
    }

    @Test
    fun `no warning is shown when the battery is unrestricted`() {
        whenever(systemBatteryRestrictions.isUnrestricted()) doReturn true

        assertFalse(policy.shouldShowWarning())
    }

    @Test
    fun `no warning is shown once they have all been shown`() {
        whenever(settings.getTimesToShowBatteryWarning()) doReturn 0
        whenever(settings.getBatteryWarningsReset()) doReturn true

        assertFalse(policy.shouldShowWarning())
    }

    @Test
    fun `warnings built up by older versions are clamped to the maximum`() {
        whenever(settings.getTimesToShowBatteryWarning()) doReturn 500

        policy.onWarningShown()

        verify(settings).setTimesToShowBatteryWarning(2)
    }

    @Test
    fun `an install that exhausted its warnings gets one fresh allowance`() {
        storeTimesToShowBatteryWarning(0)

        val warningsShown = generateSequence { newAppOpen().showWarningIfAppropriate() }.takeWhile { it }.count()

        assertEquals(3, warningsShown)
    }

    @Test
    fun `the fresh allowance is only given once`() {
        storeTimesToShowBatteryWarning(0)
        whenever(settings.getBatteryWarningsReset()) doReturn true

        assertFalse(policy.shouldShowWarning())
    }

    @Test
    fun `a build up from older versions is not shown across many app opens`() {
        storeTimesToShowBatteryWarning(500)

        val warningsShown = generateSequence { newAppOpen().showWarningIfAppropriate() }.takeWhile { it }.count()

        assertEquals(3, warningsShown)
    }

    private fun storeTimesToShowBatteryWarning(initialValue: Int) {
        val timesToShow = AtomicInteger(initialValue)
        whenever(settings.getTimesToShowBatteryWarning()) doAnswer { timesToShow.get() }
        doAnswer { invocation ->
            timesToShow.set(invocation.getArgument(0))
            Unit
        }.whenever(settings).setTimesToShowBatteryWarning(any())

        val warningsReset = AtomicBoolean(false)
        whenever(settings.getBatteryWarningsReset()) doAnswer { warningsReset.get() }
        doAnswer { invocation ->
            warningsReset.set(invocation.getArgument(0))
            Unit
        }.whenever(settings).setBatteryWarningsReset(any())
    }

    private fun BatteryWarningPolicy.showWarningIfAppropriate(): Boolean {
        if (!shouldShowWarning()) {
            return false
        }
        onWarningShown()
        return true
    }

    private fun newAppOpen() = BatteryWarningPolicy(
        settings = settings,
        systemBatteryRestrictions = systemBatteryRestrictions,
    )
}
