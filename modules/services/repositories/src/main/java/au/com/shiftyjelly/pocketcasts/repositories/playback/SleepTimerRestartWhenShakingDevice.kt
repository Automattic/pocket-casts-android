package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.extensions.isAppForeground
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SleepTimerRestartWhenShakingDevice @Inject constructor(
    private val sleepTimer: SleepTimer,
    private var playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) : SensorEventListener {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager?
    }
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
                if (acceleration > SHAKE_THRESHOLD) {
                    onDeviceShaken()
                }
            }
        }
    }

    private fun onDeviceShaken() {
        sleepTimer.restartTimerIfIsRunning onSuccess@{
            playbackManager.updateSleepTimerStatus(running = true, sleepAfterEpisode = false)

            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to SHAKING_PHONE_VALUE))

            if (context.isAppForeground()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.player_sleep_timer_restarted_after_device_shake),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    companion object {
        private const val SHAKE_THRESHOLD = 30 // A higher value for SHAKE_THRESHOLD makes the detection less sensitive
        private const val TIME_KEY = "time"
        private const val SHAKING_PHONE_VALUE = "shaking_phone"
    }
}
