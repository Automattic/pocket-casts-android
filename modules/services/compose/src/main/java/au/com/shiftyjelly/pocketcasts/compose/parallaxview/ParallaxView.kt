package au.com.shiftyjelly.pocketcasts.compose.parallaxview

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.utils.DeviceOrientationDetector
import au.com.shiftyjelly.pocketcasts.utils.OrientationData
import java.util.LinkedList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.transform

// https://rb.gy/pwz9r
@Composable
fun ParallaxView(
    modifier: Modifier = Modifier,
    depthMultiplier: Int = 20,
    content: @Composable (Modifier, BiasAlignment) -> Unit,
) {
    val context = LocalContext.current
    val orientationFlow = remember { DeviceOrientationDetector.create(context)?.orientationData()?.runningAverage(3) ?: emptyFlow() }
    val data by orientationFlow.collectAsState(OrientationData(0f, 0f))

    Content(
        modifier = modifier,
        data = data,
        depthMultiplier = depthMultiplier,
        content = content,
    )
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    data: OrientationData?,
    depthMultiplier: Int,
    content: @Composable (Modifier, BiasAlignment) -> Unit,
) {
    val roll by derivedStateOf { (data?.roll ?: 0f) * depthMultiplier }
    val pitch by derivedStateOf { (data?.pitch ?: 0f) * depthMultiplier }

    val animationSpec = remember {
        spring<Int>(stiffness = Spring.StiffnessMediumLow)
    }

    val x by animateIntAsState(
        targetValue = with(LocalDensity.current) {
            (roll * 1.5).dp.roundToPx()
        },
        animationSpec = animationSpec,
        label = "x",
    )
    val y by animateIntAsState(
        targetValue = with(LocalDensity.current) {
            -(pitch * 2).dp.roundToPx()
        },
        animationSpec = animationSpec,
        label = "y",
    )

    Box(modifier = modifier) {
        content(
            Modifier
                .offset {
                    IntOffset(
                        x = x,
                        y = y,
                    )
                },
            BiasAlignment(
                horizontalBias = (roll * 0.005).toFloat(),
                verticalBias = 0f,
            ),
        )
    }
}

private fun Flow<OrientationData>.runningAverage(windowSize: Int): Flow<OrientationData> {
    require(windowSize > 0) { "Window size must be positive" }
    return transform {
        val buffer = LinkedList<OrientationData>()
        collect { value ->
            if (buffer.size == windowSize) {
                buffer.removeFirst()
            }
            buffer.add(value)
            val bufferAverage = averageOrientationData(buffer)
            emit(bufferAverage)
        }
    }
}

private fun averageOrientationData(orientationDataList: List<OrientationData>): OrientationData {
    require(orientationDataList.isNotEmpty()) { "Sensor data list must not be empty" }

    val totalRoll = orientationDataList.sumOf { it.roll.toDouble() }
    val totalPitch = orientationDataList.sumOf { it.pitch.toDouble() }

    val averageRoll = (totalRoll / orientationDataList.size).toFloat()
    val averagePitch = (totalPitch / orientationDataList.size).toFloat()

    return OrientationData(averageRoll, averagePitch)
}
