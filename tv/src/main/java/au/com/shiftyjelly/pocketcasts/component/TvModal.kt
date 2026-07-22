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
import androidx.compose.runtime.SideEffect
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
        TvModalWindowEffects()
        val shape = RoundedCornerShape(28.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
            modifier = modifier
                .width(400.dp)
                .clip(shape)
                .background(TvColors.Dark.copy(alpha = if (isBlurBehindSupported) 0.6f else 0.94f))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                .padding(horizontal = 53.dp, vertical = 40.dp),
        )
    }
}

@Composable
private fun TvModalWindowEffects() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val blurRadius = with(LocalDensity.current) { 60.dp.roundToPx() }
    SideEffect {
        window.setDimAmount(0.55f)
        if (isBlurBehindSupported) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                blurBehindRadius = blurRadius
            }
        }
    }
}

private val isBlurBehindSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
