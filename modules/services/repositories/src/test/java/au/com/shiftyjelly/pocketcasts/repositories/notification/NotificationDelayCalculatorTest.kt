package au.com.shiftyjelly.pocketcasts.repositories.notification

import java.util.Calendar
import junit.framework.TestCase.assertEquals
import org.junit.Test

class NotificationDelayCalculatorTest {

    private val calculator = NotificationDelayCalculator()

    companion object {
        private const val HOUR_IN_MILLIS = 60 * 60 * 1000L
    }

    @Test
    fun testBefore10AM_Sync() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 0 * 24) * HOUR_IN_MILLIS // 1 hour
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Sync, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testBefore10AM_Import() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 1 * 24) * HOUR_IN_MILLIS // 25 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Import, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testBefore10AM_UpNext() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 2 * 24) * HOUR_IN_MILLIS // 49 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.UpNext, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testBefore10AM_Filters() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 3 * 24) * HOUR_IN_MILLIS // 73 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Filters, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testBefore10AM_Themes() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 4 * 24) * HOUR_IN_MILLIS // 97 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Themes, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testBefore10AM_StaffPicks() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 5 * 24) * HOUR_IN_MILLIS // 121 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.StaffPicks, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testBefore10AM_PlusUpsell() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val expectedDelay = (1 + 6 * 24) * HOUR_IN_MILLIS // 145 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.PlusUpsell, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_Sync() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 0 * 24) * HOUR_IN_MILLIS // 24 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Sync, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_Import() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 1 * 24) * HOUR_IN_MILLIS // 48 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Import, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_UpNext() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 2 * 24) * HOUR_IN_MILLIS // 72 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.UpNext, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_Filters() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 3 * 24) * HOUR_IN_MILLIS // 96 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Filters, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_Themes() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 4 * 24) * HOUR_IN_MILLIS // 120 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Themes, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_StaffPicks() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 5 * 24) * HOUR_IN_MILLIS // 144 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.StaffPicks, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testExact10AM_PlusUpsell() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val expectedDelay = (24 + 6 * 24) * HOUR_IN_MILLIS // 168 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.PlusUpsell, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_Sync() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 0 * 24) * HOUR_IN_MILLIS // 23 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Sync, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_Import() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 1 * 24) * HOUR_IN_MILLIS // 47 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Import, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_UpNext() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 2 * 24) * HOUR_IN_MILLIS // 71 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.UpNext, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_Filters() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 3 * 24) * HOUR_IN_MILLIS // 95 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Filters, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_Themes() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 4 * 24) * HOUR_IN_MILLIS // 119 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.Themes, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_StaffPicks() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 5 * 24) * HOUR_IN_MILLIS // 143 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.StaffPicks, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    @Test
    fun testAfter10AM_PlusUpsell() {
        val fixedTime = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val expectedDelay = (23 + 6 * 24) * HOUR_IN_MILLIS // 167 hours
        val delay = calculator.calculateDelayForOnboardingNotification(OnboardingNotificationType.PlusUpsell, fixedTime)
        assertEquals(expectedDelay, delay)
    }

    private fun getFixedTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        millisecond: Int = 0,
    ): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, millisecond)
        }.timeInMillis
    }
}
