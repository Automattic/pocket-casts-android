package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtMostMediumHeight
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtMostMediumWidth

object ReferralPageDefaults {
    fun shouldShowFullScreen(
        windowSizeClass: WindowSizeClass,
    ) = windowSizeClass.isAtMostMediumHeight() || windowSizeClass.isAtMostMediumWidth()

    fun pageCornerRadius(showFullScreen: Boolean) = if (showFullScreen) 0.dp else 8.dp

    const val PAGE_WIDTH_PERCENT = 0.5
}
