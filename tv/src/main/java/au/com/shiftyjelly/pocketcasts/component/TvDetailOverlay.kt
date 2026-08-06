package au.com.shiftyjelly.pocketcasts.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import au.com.shiftyjelly.pocketcasts.theme.TvDetailTopInset
import au.com.shiftyjelly.pocketcasts.theme.TvScreenBackgroundBrush

/**
 * Fades a detail screen in over the top of the still-composed parent it was opened from, so the
 * parent keeps its scroll position and focus and never reflows. The overlay is opaque (it carries
 * the screen background), hides the top bar while shown, and handles Back.
 *
 * [target] drives visibility: non-null shows the overlay, null fades it out. The last non-null
 * value is retained so [content] still renders during the exit animation. Render this as the last
 * child of the tab's root [Box] and mark the layers behind it inactive with [tvFocusInactiveWhen]
 * so the D-pad can't reach them.
 */
@Composable
fun <T> TvDetailOverlay(
    target: T?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val visible = target != null
    if (visible) {
        HideTvTopBar()
        BackHandler(onBack = onBack)
    }
    var retained by remember { mutableStateOf<T?>(null) }
    if (target != null) {
        retained = target
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = ANIMATION_MILLIS, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(durationMillis = ANIMATION_MILLIS, easing = FastOutSlowInEasing)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvScreenBackgroundBrush)
                .padding(top = TvDetailTopInset),
        ) {
            retained?.let { content(it) }
        }
    }
}

/** Deactivates focus for this subtree while [inactive] so a covered layer can't steal the D-pad. */
fun Modifier.tvFocusInactiveWhen(inactive: Boolean): Modifier = if (inactive) {
    focusProperties { canFocus = false }
} else {
    this
}

private const val ANIMATION_MILLIS = 300
