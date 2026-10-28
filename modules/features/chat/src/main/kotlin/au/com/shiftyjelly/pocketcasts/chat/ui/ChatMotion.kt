package au.com.shiftyjelly.pocketcasts.chat.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val LocalIsReduceMotionEnabled = staticCompositionLocalOf { false }

@Composable
internal fun rememberIsReduceMotionEnabled(): Boolean = LocalIsReduceMotionEnabled.current

@Composable
internal fun ProvideReduceMotion(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var isReduceMotionEnabled by remember(context) { mutableStateOf(context.isReduceMotionEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isReduceMotionEnabled = context.isReduceMotionEnabled()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalIsReduceMotionEnabled provides isReduceMotionEnabled, content = content)
}

private fun Context.isReduceMotionEnabled() = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
internal fun ChatAppear(
    animate: Boolean,
    modifier: Modifier = Modifier,
    rise: Dp = 0.dp,
    durationMillis: Int = APPEAR_DURATION_MS,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val progress = remember { Animatable(if (animate && !isReduceMotionEnabled) 0f else 1f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = EaseOut))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * rise.toPx()
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) {
        content()
    }
}

@Composable
internal fun ChatFadeOut(
    durationMillis: Int,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
    content: @Composable () -> Unit,
) {
    val visibility = remember { MutableTransitionState(true).apply { targetState = false } }
    AnimatedVisibility(
        visibleState = visibility,
        enter = EnterTransition.None,
        exit = if (rememberIsReduceMotionEnabled()) ExitTransition.None else fadeOut(tween(durationMillis, delayMillis = delayMillis, easing = easing)),
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
internal fun ChatFlight(
    fromY: Float,
    toY: Float,
    onLand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (progress: Float) -> Unit,
) {
    val currentOnLand by rememberUpdatedState(onLand)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(durationMillis = FLY_IN_DURATION_MS, easing = EaseInOut))
        currentOnLand()
    }
    Box(
        modifier = modifier.offset { IntOffset(0, lerp(fromY, toY, progress.value).roundToInt()) },
    ) {
        content(progress.value)
    }
}

internal class PlacedCoordinates {
    private var coordinates: LayoutCoordinates? = null

    fun onPlaced(placed: LayoutCoordinates) {
        coordinates = placed
    }

    fun topInRoot() = coordinates?.takeIf { it.isAttached }?.positionInRoot()?.y ?: 0f
}

internal fun flightChipMorph(progress: Float) = 1f - ((progress - FLY_IN_FILL_START) / FLY_IN_FILL_FRACTION).coerceIn(0f, 1f)

@Composable
internal fun ChatShimmerText(
    text: String,
    color: Color,
    highlight: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(isReduceMotionEnabled) {
        sweep.snapTo(0f)
        if (isReduceMotionEnabled) return@LaunchedEffect
        delay(SHIMMER_START_DELAY_MS)
        while (true) {
            sweep.animateTo(1f, tween(durationMillis = SHIMMER_SWEEP_MS, easing = LinearEasing))
            delay(SHIMMER_PAUSE_MS)
            sweep.snapTo(0f)
        }
    }
    val textLayout = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result -> textLayout.value = result },
        modifier = modifier
            .wrapContentWidth(Alignment.Start)
            .drawWithContent {
                val layout = textLayout.value ?: return@drawWithContent
                val halfBand = ShimmerHalfBandWidth.toPx()
                val center = lerp(-halfBand, size.width + halfBand, sweep.value)
                drawText(
                    textLayoutResult = layout,
                    brush = Brush.horizontalGradient(
                        colors = listOf(color, highlight, color),
                        startX = center - halfBand,
                        endX = center + halfBand,
                    ),
                )
            },
    )
}

private val ShimmerHalfBandWidth = 20.dp
private const val SHIMMER_START_DELAY_MS = 400L
private const val SHIMMER_SWEEP_MS = 870
private const val SHIMMER_PAUSE_MS = 730L

private const val APPEAR_DURATION_MS = 200
internal const val FLY_IN_DURATION_MS = 450
private const val FLY_IN_FILL_START = 0.45f
private const val FLY_IN_FILL_FRACTION = 0.25f
internal const val ANSWER_CROSS_FADE_MS = 150
