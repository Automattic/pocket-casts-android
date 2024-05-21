package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent.PLAYER_SLEEP_TIMER_RESTARTED
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.isAppForeground
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Singleton
class SleepTimerRestartWhenShakingDevice @Inject constructor(
    private val sleepTimer: SleepTimer,
    private var playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
) : SensorEventListener {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager?
    }

    private var lastTrackTime: Duration? = null

    fun init() {
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Calculate acceleration magnitude
                val acceleration = sqrt((x * x + y * y + z * z).toDouble())

                // If acceleration is above a certain threshold, consider it a shake
                if (acceleration > settings.getSleepTimerDeviceShakeThreshold()) {
                    onDeviceShaken()
                }
            }
        }
    }

    private fun onDeviceShaken() {
        val time = sleepTimer.restartTimerIfIsRunning onSuccess@{
            playbackManager.updateSleepTimerStatus(sleepTimeRunning = true)

            if (context.isAppForeground()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.player_sleep_timer_restarted_after_device_shake),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                playbackManager.playSleepTimeTone()
            }
        }
        time?.let {
            LogBuffer.i(SleepTimer.TAG, "Restarted with ${time.inWholeMinutes} minutes set after shaking device")
            trackSleepTimeRestart(it)
        }
    }

    private fun trackSleepTimeRestart(time: Duration) {
        val currentTime = System.currentTimeMillis().milliseconds
        val elapsedTime: Duration? = lastTrackTime?.let { currentTime - it }
        if (elapsedTime == null || elapsedTime >= 3.seconds) { // Make sure we don't send the same report in 3 seconds
            analyticsTracker.track(
                PLAYER_SLEEP_TIMER_RESTARTED,
                mapOf(TIME_KEY to time.inWholeSeconds, REASON_KEY to DEVICE_SHAKE_VALUE),
            )
            lastTrackTime = currentTime
        }
    }

    companion object {
        private const val TIME_KEY = "time"
        private const val REASON_KEY = "reason"
        private const val DEVICE_SHAKE_VALUE = "device_shake"
    }
}
