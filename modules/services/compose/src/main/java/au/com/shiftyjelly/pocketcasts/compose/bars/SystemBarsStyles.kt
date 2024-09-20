package au.com.shiftyjelly.pocketcasts.compose.bars

import android.content.res.Resources
import androidx.activity.SystemBarStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class SystemBarsStyles(
    val statusBarStyle: SystemBarStyle,
    val navigationBarStyle: SystemBarStyle,
)

fun SystemBarStyle.Companion.singleAuto(
    color: Color,
    detectDarkMode: (Resources) -> Boolean,
) = auto(color.toArgb(), color.toArgb(), detectDarkMode)

fun SystemBarStyle.Companion.transparent(
    detectDarkMode: (Resources) -> Boolean,
) = singleAuto(Color.Transparent, detectDarkMode)
