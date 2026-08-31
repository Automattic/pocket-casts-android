package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import kotlinx.coroutines.delay

@Stable
class TvToastHostState {
    var currentToast by mutableStateOf<TvToast?>(null)
        private set

    private var counter = 0

    fun show(message: String) {
        counter += 1
        currentToast = TvToast(id = counter, message = message)
    }

    internal fun dismiss(id: Int) {
        if (currentToast?.id == id) {
            currentToast = null
        }
    }
}

data class TvToast(
    val id: Int,
    val message: String,
)

val LocalTvToastHostState = staticCompositionLocalOf<TvToastHostState> {
    error("TvToastHostState was not provided")
}

@Composable
fun TvToastHost(
    state: TvToastHostState,
    modifier: Modifier = Modifier,
) {
    val toast = state.currentToast
    LaunchedEffect(toast?.id) {
        val id = toast?.id ?: return@LaunchedEffect
        delay(ToastDurationMillis)
        state.dismiss(id)
    }

    var lastMessage by remember { mutableStateOf("") }
    if (toast != null) {
        lastMessage = toast.message
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInHorizontally(tween(ToastAnimationMillis)) { it } + fadeIn(tween(ToastAnimationMillis)),
        exit = slideOutVertically(tween(ToastAnimationMillis)) { it / 4 } + fadeOut(tween(ToastAnimationMillis)),
        modifier = modifier,
    ) {
        TvToastContent(message = lastMessage)
    }
}

@Composable
private fun TvToastContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        color = MaterialTheme.tvColors.textPrimary,
        style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .widthIn(max = ToastMaxWidth)
            .clip(ToastShape)
            .background(MaterialTheme.tvColors.overlayContainer)
            .border(1.dp, MaterialTheme.tvColors.overlayBorder, ToastShape)
            .padding(horizontal = 22.5.dp, vertical = 15.dp),
    )
}

private val ToastDurationMillis = 3000L
private val ToastAnimationMillis = 300
private val ToastMaxWidth = 450.dp
private val ToastShape = RoundedCornerShape(12.dp)

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvToastPreview() {
    TvTheme {
        TvToastContent(message = "Marked as played")
    }
}
