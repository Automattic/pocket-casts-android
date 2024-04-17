package au.com.shiftyjelly.pocketcasts.widget.ui

import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.wrapContentSize
import au.com.shiftyjelly.pocketcasts.widget.R

@Composable
internal fun NonScalingText(
    text: String,
    textSize: Dp,
    useDynamicColors: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    alpha: Double = 1.0,
    maxLines: Int = 1,
    isBold: Boolean = false,
) {
    val normalizedMaxLines = maxLines.coerceIn(1, 2)
    val normalizedAlpha = alpha.toFloat().coerceIn(0f, 1f)

    val remoteView = RemoteViews(LocalContext.current.packageName, remoteViewId(useDynamicColors, isBold))

    with(remoteView) {
        setTextViewText(R.id.nonScalingText, text)
        setTextViewTextSize(R.id.nonScalingText, COMPLEX_UNIT_DIP, textSize.value)
        setInt(R.id.nonScalingText, "setMaxLines", normalizedMaxLines)
        setFloat(R.id.nonScalingText, "setAlpha", normalizedAlpha)
    }
    AndroidRemoteViews(
        remoteViews = remoteView,
        modifier = modifier.wrapContentSize(),
    )
}

private fun remoteViewId(
    useDynamicColors: Boolean,
    isBold: Boolean,
) = if (useDynamicColors) {
    if (isBold) R.layout.non_scaling_text_dynamic_bold else R.layout.non_scaling_text_dynamic
} else {
    if (isBold) R.layout.non_scaling_text_bold else R.layout.non_scaling_text
}
