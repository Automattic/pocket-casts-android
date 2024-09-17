package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.dp

object ReferralPageDefaults {
    fun shouldShowFullScreen(
        windowWidthSizeClass: WindowWidthSizeClass,
        windowHeightSizeClass: WindowHeightSizeClass,
    ) = windowWidthSizeClass == WindowWidthSizeClass.Compact ||
        windowHeightSizeClass == WindowHeightSizeClass.Compact

    fun pageCornerRadius(showFullScreen: Boolean) = if (showFullScreen) 0.dp else 8.dp

    const val pageWidthPercent = 0.5
}
