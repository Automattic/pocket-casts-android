package au.com.shiftyjelly.pocketcasts.repositories.notification

import jakarta.inject.Inject
import java.time.Clock
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationDelayCalculator @Inject constructor(
    private val clock: Clock,
) {
    /**
     * Calculates the delay until the notification should be sent.
     *
     * @param type The notification type containing dayOffset
     * @return Delay in milliseconds until the target 10 AM
     */
    fun calculateDelayForOnboardingNotification(
        type: OnboardingNotificationType,
    ): Long {
        val now = clock.instant().toEpochMilli()
        val next10AM = calculateBase10AM(now)
        return next10AM + TimeUnit.DAYS.toMillis(type.dayOffset.toLong()) - now
    }

    private fun calculateBase10AM(currentTimeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    /**
     * Calculates the delay until the re-engagement check worker should be triggered.
     * The worker is set to run in 7 days at 4 PM.
     *
     * @return Delay in milliseconds until the next trigger time.
     */
    fun calculateDelayForReEngagementCheck(): Long {
        val now = clock.instant().toEpochMilli()
        val next4PM = calculateBase4PM(now, dayOffset = 7)
        return next4PM - now
    }

    /**
     * Calculates the delay until the next trending and recommendations worker should be triggered.
     * The worker is set to run 2 times a week and around 4pm a given day.
     * @param index the index of the given notification to be scheduled for later
     * @param numberOfNotificationsPerWeek how many notifications are planned to trigger per week. Default value is 2
     *
     * @return Delay in milliseconds until the next trigger time.
     */
    fun calculateDelayForRecommendations(index: Int, numberOfNotificationsPerWeek: Int = 2): Long {
        val now = clock.instant().toEpochMilli()
        val daysBetweenNotifications = 7 / numberOfNotificationsPerWeek
        val next4PM = calculateBase4PM(
            currentTimeMillis = now,
            dayOffset = 1 + (index * daysBetweenNotifications),
        )
        return next4PM - now
    }

    fun calculateDelayForNewFeatures(): Long {
        val currentTimeMillis = clock.instant().toEpochMilli()
        return calculateBase4PM(currentTimeMillis) - currentTimeMillis
    }

    fun calculateDelayForOffers(): Long {
        val currentTimeMillis = clock.instant().toEpochMilli()
        return calculateBase4PM(currentTimeMillis) - currentTimeMillis
    }

    private fun calculateBase4PM(currentTimeMillis: Long, dayOffset: Int = 1): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (dayOffset > 1 || timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
        }
        return calendar.timeInMillis
    }
}
