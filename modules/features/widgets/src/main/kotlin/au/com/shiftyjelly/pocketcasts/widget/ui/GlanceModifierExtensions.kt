package au.com.shiftyjelly.pocketcasts.widget.ui

import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius

internal fun GlanceModifier.cornerRadiusCompat(radius: Dp) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    cornerRadius(radius)
} else {
    this
}

internal fun GlanceModifier.applyIf(condition: Boolean, modify: (GlanceModifier) -> GlanceModifier) = if (condition) {
    modify(this)
} else {
    this
}
