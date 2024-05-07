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
    isTransparent: Boolean = false,
    isSingleLine: Boolean = true,
    isBold: Boolean = false,
) {
    val remoteView = RemoteViews(LocalContext.current.packageName, remoteViewId(isBold, useDynamicColors, isSingleLine, isTransparent))

    with(remoteView) {
        setTextViewText(R.id.nonScalingText, text)
        setTextViewTextSize(R.id.nonScalingText, COMPLEX_UNIT_DIP, textSize.value)
    }
    AndroidRemoteViews(
        remoteViews = remoteView,
        modifier = modifier.wrapContentSize(),
    )
}

// This is awful but some OEMs do not allow to set alpha with `remoteView.setFloat(R.id.nonScalingText, "setAlpha", alpha)`
// Instead they crash without a chance to catch the exception and the whole widget fails to render.
// See: https://github.com/Automattic/pocket-casts-android/issues/2096
private fun remoteViewId(
    isBold: Boolean,
    useDynamicColors: Boolean,
    isSingleLine: Boolean,
    isTransparent: Boolean,
) = if (isBold) {
    if (useDynamicColors) {
        if (isSingleLine) {
            if (isTransparent) {
                R.layout.non_scaling_text_bold_dynamic_oneline_transparent
            } else {
                R.layout.non_scaling_text_bold_dynamic_oneline_opaque
            }
        } else {
            if (isTransparent) {
                R.layout.non_scaling_text_bold_dynamic_twolines_transparent
            } else {
                R.layout.non_scaling_text_bold_dynamic_twolines_opaque
            }
        }
    } else {
        if (isSingleLine) {
            if (isTransparent) {
                R.layout.non_scaling_text_bold_nondynamic_oneline_transparent
            } else {
                R.layout.non_scaling_text_bold_nondynamic_oneline_opaque
            }
        } else {
            if (isTransparent) {
                R.layout.non_scaling_text_bold_nondynamic_twolines_transparent
            } else {
                R.layout.non_scaling_text_bold_nondynamic_twolines_opaque
            }
        }
    }
} else {
    if (useDynamicColors) {
        if (isSingleLine) {
            if (isTransparent) {
                R.layout.non_scaling_text_regular_dynamic_oneline_transparent
            } else {
                R.layout.non_scaling_text_regular_dynamic_oneline_opaque
            }
        } else {
            if (isTransparent) {
                R.layout.non_scaling_text_regular_dynamic_twolines_transparent
            } else {
                R.layout.non_scaling_text_regular_dynamic_twolines_opaque
            }
        }
    } else {
        if (isSingleLine) {
            if (isTransparent) {
                R.layout.non_scaling_text_regular_nondynamic_oneline_transparent
            } else {
                R.layout.non_scaling_text_regular_nondynamic_oneline_opaque
            }
        } else {
            if (isTransparent) {
                R.layout.non_scaling_text_regular_nondynamic_twolines_transparent
            } else {
                R.layout.non_scaling_text_regular_nondynamic_twolines_opaque
            }
        }
    }
}
