package au.com.shiftyjelly.pocketcasts.component

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors

interface TvModalScope : ColumnScope {
    fun dismiss()
}

@Composable
fun TvModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = DefaultModalWidth,
    contentPadding: PaddingValues = DefaultContentPadding,
    content: @Composable TvModalScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val backdrop = LocalTvModalBackdrop.current
    val backdropKey = remember { Any() }
    DisposableEffect(visible) {
        backdrop.setActive(backdropKey, visible)
        onDispose { backdrop.setActive(backdropKey, false) }
    }
    Dialog(
        onDismissRequest = { visible = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val transitionState = remember { MutableTransitionState(false) }
        transitionState.targetState = visible
        TvModalDialogWindowEffects()
        LaunchedEffect(transitionState.isIdle) {
            if (!visible && transitionState.isIdle && !transitionState.currentState) {
                currentOnDismissRequest()
            }
        }
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(tween(TvModalAnimationDurationMillis)) +
                scaleIn(tween(TvModalAnimationDurationMillis), initialScale = 0.92f),
            exit = fadeOut(tween(TvModalAnimationDurationMillis)) +
                scaleOut(tween(TvModalAnimationDurationMillis), targetScale = 0.92f),
        ) {
            TvModalSurface(
                isTranslucent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                width = width,
                contentPadding = contentPadding,
                modifier = modifier.onPreviewKeyEvent { visible.not() },
            ) {
                val scope = remember(this) { TvModalScopeImpl(this) { visible = false } }
                scope.content()
            }
        }
    }
}

private class TvModalScopeImpl(
    columnScope: ColumnScope,
    private val onDismiss: () -> Unit,
) : TvModalScope,
    ColumnScope by columnScope {
    override fun dismiss() = onDismiss()
}

@Composable
internal fun TvModalSurface(
    modifier: Modifier = Modifier,
    isTranslucent: Boolean = false,
    width: Dp = DefaultModalWidth,
    contentPadding: PaddingValues = DefaultContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = if (isTranslucent) {
        MaterialTheme.tvColors.translucentOverlayContainer
    } else {
        MaterialTheme.tvColors.overlayContainer
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
        modifier = modifier
            .width(width)
            .clip(ModalShape)
            .background(containerColor)
            .background(HighlightBrush)
            .border(1.dp, MaterialTheme.tvColors.overlayBorder, ModalShape)
            .padding(contentPadding),
    )
}

@Composable
private fun TvModalDialogWindowEffects() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        window.setWindowAnimations(0)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}

private val DefaultModalWidth = 300.dp
private val DefaultContentPadding = PaddingValues(horizontal = 40.dp, vertical = 30.dp)
private val ModalShape = RoundedCornerShape(21.dp)
private val HighlightBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.08f),
        Color.Transparent,
    ),
)
