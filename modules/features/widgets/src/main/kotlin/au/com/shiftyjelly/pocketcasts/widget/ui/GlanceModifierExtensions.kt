package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.glance.GlanceModifier

internal fun GlanceModifier.applyIf(condition: Boolean, modify: (GlanceModifier) -> GlanceModifier) = if (condition) {
    modify(this)
} else {
    this
}
