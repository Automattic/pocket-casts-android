package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices as AndroidDevices

object Devices {
    const val PORTRAIT_REGULAR = "spec:width=800px,height=1600px,dpi=320"
    const val PORTRAIT_SMALL = "spec:width=720px,height=1280px,dpi=320"
    const val PORTRAIT_TABLET = "spec:width=1600px,height=2560px,dpi=276"
    const val PORTRAIT_FOLDABLE = "spec:width=1840px,height=2208px,dpi=420"

    const val LANDSCAPE_REGULAR = "$PORTRAIT_REGULAR,orientation=landscape"
    const val LANDSCAPE_SMALL = "$PORTRAIT_SMALL,orientation=landscape"
    const val LANDSCAPE_TABLET = "$PORTRAIT_TABLET,orientation=landscape"
    const val LANDSCAPE_FOLDABLE = "$PORTRAIT_FOLDABLE,orientation=landscape"

    const val AUTOMOTIVE = AndroidDevices.AUTOMOTIVE_1024p
}

@Preview(device = Devices.PORTRAIT_REGULAR)
annotation class PreviewRegularDevice

@Preview(device = Devices.PORTRAIT_REGULAR)
@Preview(device = Devices.LANDSCAPE_REGULAR)
annotation class PreviewOrientation

@Preview(device = Devices.AUTOMOTIVE)
annotation class PreviewAutomotive
