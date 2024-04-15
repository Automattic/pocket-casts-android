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
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class SleepTimer @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) {

    companion object {
        private var sleepTimeMs: Long? = null
        private var lastSleepAfterTimeInMinutes: Int = 0
        private var lastTimeHasFinishedInMillis: Long = 0
        private var lastEpisodeUuidAutomaticEnded: String? = null
        private const val MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES = 5
        private const val TIME_KEY = "time"
        private const val END_OF_EPISODE_VALUE = "end_of_episode"
    }

    fun sleepAfter(mins: Int, onSuccess: () -> Unit) {
        val time = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, mins)
        }
        if (createAlarm(time.timeInMillis)) {
            lastSleepAfterTimeInMinutes = mins
            cancelAutomaticSleepOnEpisodeEndRestart()
            onSuccess()
        }
    }

    fun addExtraTime(mins: Int) {
        val currentTimeMs = sleepTimeMs
        if (currentTimeMs == null || currentTimeMs < 0) {
            return
        }
        val time = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            add(Calendar.MINUTE, mins)
        }
        createAlarm(time.timeInMillis)
    }
    fun restartSleepTimerIfApplies(currentEpisodeUuid: String, isSleepTimerRunning: Boolean, onRestartSleepAfterTime: () -> Unit, onRestartSleepOnEpisodeEnd: () -> Unit) {
        val diffTimeInMillis = System.currentTimeMillis() - lastTimeHasFinishedInMillis
        val diffInMinutes = diffTimeInMillis / (1000 * 60) // Convert to minutes

        if (diffInMinutes < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !lastEpisodeUuidAutomaticEnded.isNullOrEmpty() && currentEpisodeUuid != lastEpisodeUuidAutomaticEnded) {
            onRestartSleepOnEpisodeEnd()
            analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_EPISODE_VALUE))
        } else if (diffInMinutes < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && lastSleepAfterTimeInMinutes != 0 && !isSleepTimerRunning) {
            analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to lastSleepAfterTimeInMinutes * 60)) // Convert to seconds
            sleepAfter(lastSleepAfterTimeInMinutes, onRestartSleepAfterTime)
        }
    }
    fun setEndOfEpisodeUuid(uuid: String?) {
        lastEpisodeUuidAutomaticEnded = uuid
        lastTimeHasFinishedInMillis = System.currentTimeMillis()
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
                lastTimeHasFinishedInMillis = timeMs
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
        val sleepTimeMs = SleepTimer.sleepTimeMs ?: return null

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
        lastSleepAfterTimeInMinutes = 0
    }
    private fun cancelAutomaticSleepOnEpisodeEndRestart() {
        lastEpisodeUuidAutomaticEnded = null
    }
}
