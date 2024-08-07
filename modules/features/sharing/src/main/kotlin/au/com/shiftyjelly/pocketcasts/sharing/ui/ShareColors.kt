package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils

internal data class ShareColors(
    val base: Color,
) {
    val navigationBar = ColorUtils.changeHsvValue(base, factor = 0.15f)
    val background = ColorUtils.changeHsvValue(base, factor = 0.4f)
    val backgroundPrimaryText = if (background.luminance() < 0.5f) Color.White else Color.Black
    val backgroundSecondaryText = backgroundPrimaryText.copy(alpha = 0.5f)

    val cardTop = ColorUtils.changeHsvValue(base, 1.25f)
    val cardBottom = ColorUtils.changeHsvValue(base, 0.75f)
    val cardText = if (base.luminance() < 0.5f) Color.White else Color.Black

    val pagerIndicatorActive = if (background.luminance() < 0.5f) Color.White else Color.Black
    val pagerIndicatorInactive = pagerIndicatorActive.copy(alpha = 0.3f)

    val snackbar = if (background.luminance() < 0.5f) Color.White else Color.Black
    val snackbarText = if (snackbar.luminance() < 0.5f) Color.White else Color.Black

    val clipButton = if (background.luminance() < 0.25) {
        ColorUtils.changeHsvValue(base, 2f)
    } else {
        base
    }
    val clipButtonText = if (clipButton.luminance() < 0.5f) Color.White else Color.Black

    val closeButton = backgroundPrimaryText.copy(alpha = 0.15f)
    val closeButtonIcon = Color.White.copy(alpha = 0.5f)

    val timeline = backgroundPrimaryText.copy(alpha = 0.15f)
    val timelineProgress = backgroundPrimaryText
    val timelineTick = timelineProgress.copy(alpha = 0.4f)
    val playPauseButton = Color.White

    val selector = if (background.luminance() < 0.25) {
        ColorUtils.changeHsvValue(base, 2f)
    } else {
        base
    }
    val selectorHandle = background.copy(alpha = 0.4f)

    val socialButton = backgroundPrimaryText.copy(alpha = 0.1f)
    val socialButtonIcon = Color.White
}
