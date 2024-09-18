package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class DeviceOrientationDetector private constructor(
    private val sensorManager: SensorManager,
    private val accelerometer: Sensor,
    private val magnetometer: Sensor,
) {
    fun orientationData() = callbackFlow<OrientationData> {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GRAVITY -> gravity = event.values
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
                }

                gravity?.let { gravity ->
                    geomagnetic?.let { geomagnetic ->
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            trySend(OrientationData(roll = orientation[2], pitch = orientation[1]))
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    companion object {
        fun create(context: Context): DeviceOrientationDetector? {
            val service = context.getSystemService<SensorManager>() ?: return null
            val accelerometer = service.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: return null
            val magnetometer = service.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: return null
            return DeviceOrientationDetector(service, accelerometer, magnetometer)
        }
    }
}

data class OrientationData(
    val roll: Float,
    val pitch: Float,
)
