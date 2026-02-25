package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import com.google.android.horologist.compose.layout.ResponsiveTimeText
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
    ResponsiveTimeText(
        modifier = modifier,
        startCurvedContent = if (isConnected) null else {
            {
                curvedComposable {
                    OfflineIcon()
                }
            }
        },
        startLinearContent = if (isConnected) null else {
            {
                OfflineIcon()
            }
        },
    )
}

@Composable
private fun OfflineIcon() {
    Icon(
        painter = painterResource(IR.drawable.ic_cloud_off),
        contentDescription = "Offline icon",
        tint = MaterialTheme.colors.onBackground,
        modifier = Modifier
            .size(16.dp)
            .padding(end = 2.dp),
    )
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
private fun TimeTextWithConnectivityConnectedPreview() {
    WearAppTheme {
        TimeTextWithConnectivity(isConnected = true)
    }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
private fun TimeTextWithConnectivityDisconnectedPreview() {
    WearAppTheme {
        TimeTextWithConnectivity(isConnected = false)
    }
}
