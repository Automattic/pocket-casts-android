package au.com.shiftyjelly.pocketcasts.component

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.State
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import java.util.function.Consumer

@Composable
fun TvModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = DefaultModalWidth,
    contentPadding: PaddingValues = DefaultContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    Dialog(
        onDismissRequest = { visible = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val transitionState = remember { MutableTransitionState(false) }
        transitionState.targetState = visible
        val isBlurBehindEnabled by rememberIsBlurBehindEnabled()
        val dimAmount by animateFloatAsState(
            targetValue = if (visible) MODAL_DIM_AMOUNT else 0f,
            animationSpec = tween(MODAL_ANIMATION_MILLIS),
            label = "TvModalDim",
        )
        TvModalWindowEffects(isBlurBehindEnabled = isBlurBehindEnabled, dimAmount = dimAmount)
        LaunchedEffect(transitionState.isIdle) {
            if (!visible && transitionState.isIdle && !transitionState.currentState) {
                currentOnDismissRequest()
            }
        }
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(tween(MODAL_ANIMATION_MILLIS)) + scaleIn(tween(MODAL_ANIMATION_MILLIS), initialScale = 0.92f),
            exit = fadeOut(tween(MODAL_ANIMATION_MILLIS)) + scaleOut(tween(MODAL_ANIMATION_MILLIS), targetScale = 0.92f),
        ) {
            TvModalSurface(
                isTranslucent = isBlurBehindEnabled,
                width = width,
                contentPadding = contentPadding,
                modifier = modifier,
                content = content,
            )
        }
    }
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
private fun TvModalWindowEffects(isBlurBehindEnabled: Boolean, dimAmount: Float) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val blurRadius = with(LocalDensity.current) { ModalBlurRadius.roundToPx() }
    LaunchedEffect(window) {
        window.setWindowAnimations(0)
    }
    LaunchedEffect(window, dimAmount) {
        window.setDimAmount(dimAmount)
    }
    LaunchedEffect(window, isBlurBehindEnabled, blurRadius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isBlurBehindEnabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = blurRadius }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}

@Composable
private fun rememberIsBlurBehindEnabled(): State<Boolean> {
    val isBlurBehindEnabled = remember { mutableStateOf(false) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val view = LocalView.current
        DisposableEffect(view) {
            val windowManager = view.context.getSystemService(WindowManager::class.java)
            val listener = Consumer<Boolean> { isEnabled -> isBlurBehindEnabled.value = isEnabled }
            windowManager.addCrossWindowBlurEnabledListener(listener)
            onDispose {
                windowManager.removeCrossWindowBlurEnabledListener(listener)
            }
        }
    }
    return isBlurBehindEnabled
}

private const val MODAL_ANIMATION_MILLIS = 200
private const val MODAL_DIM_AMOUNT = 0.4f
private val ModalBlurRadius = 30.dp
private val DefaultModalWidth = 300.dp
private val DefaultContentPadding = PaddingValues(horizontal = 40.dp, vertical = 30.dp)
private val ModalShape = RoundedCornerShape(21.dp)
private val HighlightBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.08f),
        Color.Transparent,
    ),
)
