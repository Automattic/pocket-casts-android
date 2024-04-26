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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class SleepTimer @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES = 5.minutes
        private const val TIME_KEY = "time"
        private const val NUMBER_OF_EPISODES_KEY = "number_of_episodes"
        private const val END_OF_EPISODE_VALUE = "end_of_episode"
    }

    private var sleepTimeMs: Long? = null
    private var lastSleepAfterTime: Duration? = null
    private var lastSleepAfterEndOfChapterTime: Duration? = null
    private var lastTimeSleepTimeHasFinished: Duration? = null
    private var lastEpisodeUuidAutomaticEnded: String? = null

    fun sleepAfter(duration: Duration, onSuccess: () -> Unit) {
        val sleepAt = System.currentTimeMillis().milliseconds + duration

        if (createAlarm(sleepAt.inWholeMilliseconds)) {
            lastSleepAfterTime = duration
            cancelAutomaticSleepOnEpisodeEndRestart()
            cancelAutomaticSleepOnChapterEndRestart()
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

    fun restartTimerIfIsRunning(onSuccess: () -> Unit): Duration? {
        return if (isSleepAfterTimerRunning) {
            lastSleepAfterTime?.let { sleepAfter(it, onSuccess) }
            lastSleepAfterTime
        } else {
            null
        }
    }

    fun restartSleepTimerIfApplies(
        currentEpisodeUuid: String,
        isSleepTimerRunning: Boolean,
        isSleepEndOfEpisodeRunning: Boolean,
        isSleepEndOfChapterRunning: Boolean,
        numberOfEpisodes: Int,
        onRestartSleepAfterTime: () -> Unit,
        onRestartSleepOnEpisodeEnd: () -> Unit,
        onRestartSleepOnChapterEnd: () -> Unit,
    ) {
        lastTimeSleepTimeHasFinished?.let { lastTimeHasFinished ->
            val diffTime = System.currentTimeMillis().milliseconds - lastTimeHasFinished

            if (shouldRestartSleepEndOfChapter(diffTime, isSleepEndOfChapterRunning)) {
                onRestartSleepOnChapterEnd()
            } else if (shouldRestartSleepEndOfEpisode(diffTime, currentEpisodeUuid, isSleepEndOfEpisodeRunning)) {
                onRestartSleepOnEpisodeEnd()
                analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_EPISODE_VALUE, NUMBER_OF_EPISODES_KEY to numberOfEpisodes))
            } else if (shouldRestartSleepAfterTime(diffTime, isSleepTimerRunning)) {
                lastSleepAfterTime?.let {
                    analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to it.inWholeSeconds))
                    sleepAfter(it, onRestartSleepAfterTime)
                }
            }
        }
    }

    fun setEndOfEpisodeUuid(uuid: String) {
        lastEpisodeUuidAutomaticEnded = uuid
        lastTimeSleepTimeHasFinished = System.currentTimeMillis().milliseconds
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnChapterEndRestart()
    }

    fun setEndOfChapter() {
        val time = System.currentTimeMillis().milliseconds
        lastSleepAfterEndOfChapterTime = time
        lastTimeSleepTimeHasFinished = time
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnEpisodeEndRestart()
    }

    private fun shouldRestartSleepAfterTime(diffTime: Duration, isSleepTimerRunning: Boolean) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && lastSleepAfterTime != null && !isSleepTimerRunning

    private fun shouldRestartSleepEndOfEpisode(
        diffTime: Duration,
        currentEpisodeUuid: String,
        isSleepEndOfEpisodeRunning: Boolean,
    ) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !lastEpisodeUuidAutomaticEnded.isNullOrEmpty() && currentEpisodeUuid != lastEpisodeUuidAutomaticEnded && !isSleepEndOfEpisodeRunning

    private fun shouldRestartSleepEndOfChapter(diffTime: Duration, isSleepEndOfChapterRunning: Boolean) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !isSleepEndOfChapterRunning && lastSleepAfterEndOfChapterTime != null

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
                lastTimeSleepTimeHasFinished = timeMs.milliseconds
                true
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to start sleep timer.")
                false
            }
        }
    }

    fun cancelTimer() {
        getAlarmManager().cancel(getSleepIntent())
        cancelSleepTime()
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnEpisodeEndRestart()
        cancelAutomaticSleepOnChapterEndRestart()
    }

    val isSleepAfterTimerRunning: Boolean
        get() = System.currentTimeMillis() < (sleepTimeMs ?: -1)

    fun timeLeftInSecs(): Int? {
        val sleepTimeMs = sleepTimeMs ?: return null

        val timeLeft = sleepTimeMs - System.currentTimeMillis()
        if (timeLeft < 0) {
            cancelSleepTime()
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

    private fun cancelSleepTime() {
        sleepTimeMs = null
    }

    private fun cancelAutomaticSleepAfterTimeRestart() {
        lastSleepAfterTime = null
    }

    private fun cancelAutomaticSleepOnEpisodeEndRestart() {
        lastEpisodeUuidAutomaticEnded = null
    }

    private fun cancelAutomaticSleepOnChapterEndRestart() {
        lastSleepAfterEndOfChapterTime = null
    }
}
