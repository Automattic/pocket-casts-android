package au.com.shiftyjelly.pocketcasts.chat.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    content: @Composable () -> Unit,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val progress = remember { Animatable(if (animate && !isReduceMotionEnabled) 0f else 1f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(durationMillis = durationMillis, easing = EaseOut))
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
    content: @Composable () -> Unit,
) {
    if (rememberIsReduceMotionEnabled()) return
    val visibility = remember { MutableTransitionState(true) }
    visibility.targetState = false
    AnimatedVisibility(
        visibleState = visibility,
        enter = EnterTransition.None,
        exit = fadeOut(tween(durationMillis)),
        modifier = modifier,
    ) {
        content()
    }
}

private const val APPEAR_DURATION_MS = 200
internal const val ANSWER_CROSS_FADE_MS = 150
