package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils

internal data class ShareColors(
    val base: Color,
) {
    val navigationBar = ColorUtils.changeHsvValue(base, factor = 0.15f)
    val background = ColorUtils.changeHsvValue(base, factor = 0.4f)
    val onBackgroundPrimary = if (background.isDark) Color.White else Color.Black
    val onBackgroundSecondary = onBackgroundPrimary.copy(alpha = 0.5f)

    val accent = if (background.isVeryDark) ColorUtils.changeHsvValue(base, 2f) else base
    val onAccent = if (accent.isDark) Color.White else Color.Black

    val container = (if (background.isDark) Color.White else Color.Black).copy(alpha = 0.15f)
    val onContainerPrimary = if (background.isDark) Color.White else Color.Black
    val onContainerSecondary = onContainerPrimary.copy(alpha = 0.5f)

    val cardBottom = ColorUtils.changeHsvValue(base, 0.6f)
    val cardTop = ColorUtils.changeHsvValue(base, 0.75f)
    val cardTextPrimary = if (base.isDark) Color.White else Color.Black
    val cardTextSecondary = cardTextPrimary.copy(alpha = 0.5f)

    val snackbar = if (background.isDark) Color.White else Color.Black
    val snackbarText = if (snackbar.isDark) Color.White else Color.Black
}

private val Color.isDark get() = luminance() < 0.5f
private val Color.isVeryDark get() = luminance() < 0.25f
