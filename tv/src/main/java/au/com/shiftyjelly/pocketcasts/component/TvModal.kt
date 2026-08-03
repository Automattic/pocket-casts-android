package au.com.shiftyjelly.pocketcasts.component

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import java.util.function.Consumer

@Composable
fun TvModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val isBlurBehindEnabled by rememberIsBlurBehindEnabled()
        TvModalWindowEffects(isBlurBehindEnabled = isBlurBehindEnabled)
        TvModalSurface(
            isTranslucent = isBlurBehindEnabled,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
internal fun TvModalSurface(
    modifier: Modifier = Modifier,
    isTranslucent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
        modifier = modifier
            .width(400.dp)
            .clip(ModalShape)
            .background(if (isTranslucent) TranslucentContainerColor else OpaqueContainerColor)
            .background(HighlightBrush)
            .border(1.dp, BorderColor, ModalShape)
            .padding(horizontal = 53.dp, vertical = 40.dp),
    )
}

@Composable
private fun TvModalWindowEffects(isBlurBehindEnabled: Boolean) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val blurRadius = with(LocalDensity.current) { 60.dp.roundToPx() }
    LaunchedEffect(window, isBlurBehindEnabled, blurRadius) {
        window.setDimAmount(0.55f)
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

private val ModalShape = RoundedCornerShape(28.dp)
private val TranslucentContainerColor = TvColors.Dark.copy(alpha = 0.6f)
private val OpaqueContainerColor = TvColors.Dark.copy(alpha = 0.94f)
private val BorderColor = Color.White.copy(alpha = 0.12f)
private val HighlightBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.08f),
        Color.Transparent,
    ),
)
