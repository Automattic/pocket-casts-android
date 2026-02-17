package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.curvedText
import au.com.shiftyjelly.pocketcasts.images.R as IR

/**
 * Custom TimeText composable that displays the system time along with
 * an offline indicator icon when the device is not connected to the internet.
 * Uses curved layout to follow the watch bezel curvature.
 *
 * @param isConnected Whether the device is connected to WiFi or cellular data
 * @param modifier Optional modifier for the component
 */
@Composable
fun TimeTextWithConnectivity(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colors.onBackground

    CurvedLayout(
        modifier = modifier,
        anchor = 270f,
        angularDirection = CurvedDirection.Angular.Clockwise,
    ) {
        if (!isConnected) {
            curvedComposable {
                Icon(
                    painter = painterResource(IR.drawable.ic_cloud_off),
                    contentDescription = "Offline",
                    tint = textColor,
                    modifier = Modifier.size(16.dp).padding(end = 2.dp),
                )
            }
            curvedText(
                text = " · ",
                style = CurvedTextStyle(
                    fontSize = 16.sp,
                    color = textColor,
                ),
            )
        }
        curvedComposable {
            TimeText()
        }
    }
}
