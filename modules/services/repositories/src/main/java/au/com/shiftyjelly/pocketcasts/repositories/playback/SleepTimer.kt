package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.text.format.DateUtils
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent.PLAYER_SLEEP_TIMER_RESTARTED
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class SleepTimer @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES = 5
        private const val TIME_KEY = "time"
        private const val END_OF_EPISODE_VALUE = "end_of_episode"
    }

    private var sleepTimeMs: Long? = null
    private var lastSleepAfterTime: Duration? = null
    private var lastTimeSleepTimeHasFinished: Duration? = null
    private var lastEpisodeUuidAutomaticEnded: String? = null

    fun sleepAfter(minutes: Int, onSuccess: () -> Unit) {
        val time = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, minutes)
        }
        if (createAlarm(time.timeInMillis)) {
            lastSleepAfterTime = minutes.toDuration(DurationUnit.MINUTES)
            cancelAutomaticSleepOnEpisodeEndRestart()
            onSuccess()
        }
    }

    fun addExtraTime(minutes: Int) {
        val currentTimeMs = sleepTimeMs
        if (currentTimeMs == null || currentTimeMs < 0) {
            return
        }
        val time = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            add(Calendar.MINUTE, minutes)
        }
        createAlarm(time.timeInMillis)
    }
    fun restartSleepTimerIfApplies(currentEpisodeUuid: String, isSleepTimerRunning: Boolean, onRestartSleepAfterTime: () -> Unit, onRestartSleepOnEpisodeEnd: () -> Unit) {
        lastTimeSleepTimeHasFinished?.let { lastTimeHasFinished ->
            val diffTimeInMillis = System.currentTimeMillis() - lastTimeHasFinished.inWholeMilliseconds
            val diffInMinutes = diffTimeInMillis / (1000 * 60) // Convert to minutes

            if (diffInMinutes < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !lastEpisodeUuidAutomaticEnded.isNullOrEmpty() && currentEpisodeUuid != lastEpisodeUuidAutomaticEnded) {
                onRestartSleepOnEpisodeEnd()
                analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_EPISODE_VALUE))
            } else if (diffInMinutes < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && lastSleepAfterTime != null && !isSleepTimerRunning) {
                lastSleepAfterTime?.let {
                    analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to it * 60)) // Convert to seconds
                    sleepAfter(it.inWholeMinutes.toInt(), onRestartSleepAfterTime)
                }
            }
        }
    }
    fun setEndOfEpisodeUuid(uuid: String) {
        lastEpisodeUuidAutomaticEnded = uuid
        lastTimeSleepTimeHasFinished = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)
        cancelAutomaticSleepAfterTimeRestart()
    }

    private fun createAlarm(timeMs: Long): Boolean {
        val sleepIntent = getSleepIntent()
        val alarmManager = getAlarmManager()
        alarmManager.cancel(sleepIntent)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, LR.string.player_sleep_timer_start_failed, Toast.LENGTH_LONG).show()
            context.startActivity(
                Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    // Because we're not launching this from an activity context, we must add the FLAG_ACTIVITY_NEW_TASK flag
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            false
        } else {
            return try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, sleepIntent)
                sleepTimeMs = timeMs
                lastTimeSleepTimeHasFinished = timeMs.toDuration(DurationUnit.MILLISECONDS)
                true
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to start sleep timer.")
                false
            }
        }
    }

    fun cancelTimer() {
        getAlarmManager().cancel(getSleepIntent())
        cleanUpSleepTimer()
    }

    val isRunning: Boolean
        get() = System.currentTimeMillis() < (sleepTimeMs ?: -1)

    fun timeLeftInSecs(): Int? {
        val sleepTimeMs = sleepTimeMs ?: return null

        val timeLeft = sleepTimeMs - System.currentTimeMillis()
        if (timeLeft < 0) {
            cleanUpSleepTimer()
            return null
        }
        return (timeLeft / DateUtils.SECOND_IN_MILLIS).toInt()
    }

    private fun getSleepIntent(): PendingIntent {
        val intent = Intent(context, SleepTimerReceiver::class.java)
        return PendingIntent.getBroadcast(context, 234324243, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getAlarmManager(): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun cleanUpSleepTimer() {
        sleepTimeMs = null
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnEpisodeEndRestart()
    }
    private fun cancelAutomaticSleepAfterTimeRestart() {
        lastSleepAfterTime = null
    }
    private fun cancelAutomaticSleepOnEpisodeEndRestart() {
        lastEpisodeUuidAutomaticEnded = null
    }
}
