package au.com.shiftyjelly.pocketcasts.repositories.notification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import org.junit.Test

class NotificationDelayCalculatorTest {

    companion object {
        private const val HOUR_IN_MILLIS = 60 * 60 * 1000L

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

        private fun calculatorAt(fixedTime: Long): NotificationDelayCalculator {
            val fixedInstant = Instant.ofEpochMilli(fixedTime)
            val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
            return NotificationDelayCalculator(fixedClock)
        }
    }

    @Test fun testBefore10AM_Sync() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 0 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Sync))
    }

    @Test fun testBefore10AM_Import() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 1 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Import))
    }

    @Test fun testBefore10AM_UpNext() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 2 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.UpNext))
    }

    @Test fun testBefore10AM_Filters() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 3 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Filters))
    }

    @Test fun testBefore10AM_Themes() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 4 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Themes))
    }

    @Test fun testBefore10AM_StaffPicks() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 5 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.StaffPicks))
    }

    @Test fun testBefore10AM_PlusUpsell() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 9, 0)
        val calc = calculatorAt(t)
        val expected = (1 + 6 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.PlusUpsell))
    }

    @Test fun testExact10AM_Sync() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 0 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Sync))
    }

    @Test fun testExact10AM_Import() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 1 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Import))
    }

    @Test fun testExact10AM_UpNext() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 2 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.UpNext))
    }

    @Test fun testExact10AM_Filters() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 3 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Filters))
    }

    @Test fun testExact10AM_Themes() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 4 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Themes))
    }

    @Test fun testExact10AM_StaffPicks() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 5 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.StaffPicks))
    }

    @Test fun testExact10AM_PlusUpsell() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 10, 0)
        val calc = calculatorAt(t)
        val expected = (24 + 6 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.PlusUpsell))
    }

    @Test fun testAfter10AM_Sync() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 0 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Sync))
    }

    @Test fun testAfter10AM_Import() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 1 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Import))
    }

    @Test fun testAfter10AM_UpNext() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 2 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.UpNext))
    }

    @Test fun testAfter10AM_Filters() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 3 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Filters))
    }

    @Test fun testAfter10AM_Themes() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 4 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.Themes))
    }

    @Test fun testAfter10AM_StaffPicks() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 5 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.StaffPicks))
    }

    @Test fun testAfter10AM_PlusUpsell() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 11, 0)
        val calc = calculatorAt(t)
        val expected = (23 + 6 * 24) * HOUR_IN_MILLIS
        assertEquals(expected, calc.calculateDelayForOnboardingNotification(OnboardingNotificationType.PlusUpsell))
    }

    @Test fun testReEngagementCheck_Before4PM() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 15, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForReEngagementCheck()
        val calculatedTriggerTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.APRIL, 17, 16, 0)
        assertEquals(expected, calculatedTriggerTime)
    }

    @Test fun testReEngagementCheck_Exactly4PM() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 16, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForReEngagementCheck()
        val calculatedTriggerTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.APRIL, 17, 16, 0)
        assertEquals(expected, calculatedTriggerTime)
    }

    @Test fun testReEngagementCheck_After4PM() {
        val t = getFixedTime(2025, Calendar.APRIL, 10, 17, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForReEngagementCheck()
        val calculatedTriggerTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.APRIL, 17, 16, 0)
        assertEquals(expected, calculatedTriggerTime)
    }

    @Test fun testRecommendations_Before4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 15, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForRecommendations(0, 2)
        val calculatedTrendingTriggerTime = t + calculatedDelay
        val expectedTrendingTime = getFixedTime(2025, Calendar.MAY, 21, 16, 0)
        assertEquals(expectedTrendingTime, calculatedTrendingTriggerTime)

        val calculatedRecommendationsDelay = calculatorAt(t).calculateDelayForRecommendations(1, 2)
        val calculatedRecommendationsTriggerTime = t + calculatedRecommendationsDelay
        val expectedRecommendationsTime = getFixedTime(2025, Calendar.MAY, 25, 16, 0)
        assertEquals(expectedRecommendationsTime, calculatedRecommendationsTriggerTime)
    }

    @Test fun testRecommendations_At4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 16, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForRecommendations(0, 2)
        val calculatedTriggerTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.MAY, 22, 16, 0)
        assertEquals(expected, calculatedTriggerTime)

        val calculatedRecommendationsDelay = calculatorAt(t).calculateDelayForRecommendations(1, 2)
        val calculatedRecommendationsTriggerTime = t + calculatedRecommendationsDelay
        val expectedRecommendationsTime = getFixedTime(2025, Calendar.MAY, 25, 16, 0)
        assertEquals(expectedRecommendationsTime, calculatedRecommendationsTriggerTime)
    }

    @Test fun testRecommendations_After4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 17, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForRecommendations(0, 2)
        val calculatedTriggerTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.MAY, 22, 16, 0)
        assertEquals(expected, calculatedTriggerTime)

        val calculatedRecommendationsDelay = calculatorAt(t).calculateDelayForRecommendations(1, 2)
        val calculatedRecommendationsTriggerTime = t + calculatedRecommendationsDelay
        val expectedRecommendationsTime = getFixedTime(2025, Calendar.MAY, 25, 16, 0)
        assertEquals(expectedRecommendationsTime, calculatedRecommendationsTriggerTime)
    }

    @Test fun testFeaturesAndTips_Before4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 15, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForNewFeatures()
        val calculatedTime = t + calculatedDelay
        val expectedTime = getFixedTime(2025, Calendar.MAY, 21, 16, 0)
        assertEquals(expectedTime, calculatedTime)
    }

    @Test fun testFeaturesAndTips_At4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 16, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForNewFeatures()
        val calculatedTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.MAY, 22, 16, 0)
        assertEquals(expected, calculatedTime)
    }

    @Test fun testFeaturesAndTips_After4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 17, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForNewFeatures()
        val calculatedTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.MAY, 22, 16, 0)
        assertEquals(expected, calculatedTime)
    }

    @Test fun testOffers_Before4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 15, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForOffers()
        val calculatedTriggerTime = t + calculatedDelay
        val expectedTime = getFixedTime(2025, Calendar.MAY, 21, 16, 0)
        assertEquals(expectedTime, calculatedTriggerTime)
    }

    @Test fun testOffers_At4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 16, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForOffers()
        val calculatedTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.MAY, 22, 16, 0)
        assertEquals(expected, calculatedTime)
    }

    @Test fun testOffers_After4PM() {
        val t = getFixedTime(2025, Calendar.MAY, 21, 17, 0)
        val calculatedDelay = calculatorAt(t).calculateDelayForOffers()
        val calculatedTime = t + calculatedDelay
        val expected = getFixedTime(2025, Calendar.MAY, 22, 16, 0)
        assertEquals(expected, calculatedTime)
    }
}
