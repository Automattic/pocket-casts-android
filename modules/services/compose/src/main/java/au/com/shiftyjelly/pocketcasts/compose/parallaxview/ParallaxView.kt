package au.com.shiftyjelly.pocketcasts.compose.parallaxview

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.util.LinkedList

// https://rb.gy/pwz9r
@Composable
fun ParallaxView(
    modifier: Modifier = Modifier,
    depthMultiplier: Int = 20,
    content: @Composable (Modifier, BiasAlignment) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<SensorData?>(null) }

    Content(
        modifier = modifier,
        data = data,
        depthMultiplier = depthMultiplier,
        content = content,
    )

    DisposableEffect(Unit) {
        val dataManager = SensorDataManager(context)
        dataManager.init()

        val job = scope.launch {
            dataManager.data
                .receiveAsFlow()
                .runningAverage(3)
                .collect { data = it }
        }

        onDispose {
            dataManager.cancel()
            job.cancel()
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    data: SensorData?,
    depthMultiplier: Int,
    content: @Composable (Modifier, BiasAlignment) -> Unit,
) {
    val roll by derivedStateOf { (data?.roll ?: 0f) * depthMultiplier }
    val pitch by derivedStateOf { (data?.pitch ?: 0f) * depthMultiplier }

    Box(modifier = modifier) {
        content(
            Modifier
                .offset {
                    IntOffset(
                        x = (roll * 1.5).dp.roundToPx(),
                        y = -(pitch* 2).dp.roundToPx()
                    )
                },
            BiasAlignment(
                horizontalBias = (roll * 0.005).toFloat(),
                verticalBias = 0f,
            )
        )
    }
}

private fun Flow<SensorData>.runningAverage(windowSize: Int): Flow<SensorData> {
    require(windowSize > 0) { "Window size must be positive" }
    return transform {
        val buffer = LinkedList<SensorData>()
        collect { value ->
            if (buffer.size == windowSize) {
                buffer.removeFirst()
            }
            buffer.add(value)
            val bufferAverage = averageSensorData(buffer)
            emit(bufferAverage)
        }
    }
}

private fun averageSensorData(sensorDataList: List<SensorData>): SensorData {
    require(sensorDataList.isNotEmpty()) { "Sensor data list must not be empty" }

    val totalRoll = sensorDataList.sumOf { it.roll.toDouble() }
    val totalPitch = sensorDataList.sumOf { it.pitch.toDouble() }

    val averageRoll = (totalRoll / sensorDataList.size).toFloat()
    val averagePitch = (totalPitch / sensorDataList.size).toFloat()

    return SensorData(averageRoll, averagePitch)
}
