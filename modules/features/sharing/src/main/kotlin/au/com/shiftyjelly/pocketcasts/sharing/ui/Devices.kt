package au.com.shiftyjelly.pocketcasts.sharing.ui

internal object Devices {
    const val PortraitRegular = "spec:width=800px,height=1600px,dpi=320"
    const val PortraitSmall = "spec:width=720px,height=1280px,dpi=320"
    const val PortraitTablet = "spec:width=1600px,height=2560px,dpi=276"

    const val LandscapeRegular = "$PortraitRegular,orientation=landscape"
    const val LandscapeSmall = "$PortraitSmall,orientation=landscape"
    const val LandscapeTablet = "$PortraitTablet,orientation=landscape"
}
