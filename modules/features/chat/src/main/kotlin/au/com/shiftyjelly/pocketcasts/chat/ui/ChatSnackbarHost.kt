package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.ThemedSnackbarHost

@Composable
internal fun ChatSnackbarHost(
    hostState: SnackbarHostState,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    ThemedSnackbarHost(
        hostState = hostState,
        snackbar = { data, _ ->
            key(data) {
                SwipeToDismissToast(onDismiss = data::dismiss) {
                    ChatSnackbar(data = data, theme = theme)
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ChatSnackbar(
    data: SnackbarData,
    theme: ChatTheme,
) {
    Snackbar(
        backgroundColor = Color.White,
        contentColor = theme.onWhiteText,
        action = data.actionLabel?.let { label ->
            {
                TextButton(onClick = data::performAction) {
                    Text(
                        text = label,
                        color = theme.toastAction,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        elevation = 0.dp,
        modifier = if (data.actionLabel != null || data.isProgress) Modifier.heightIn(min = 56.dp) else Modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (data.isProgress) {
                ToastSpinner(color = theme.toastAction)
            }
            Text(
                text = data.message,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val SnackbarData.isProgress get() = actionLabel == null && duration == SnackbarDuration.Indefinite

@Composable
private fun ToastSpinner(
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (rememberIsReduceMotionEnabled()) {
        CircularProgressIndicator(
            progress = REDUCED_MOTION_SPINNER_PROGRESS,
            color = color,
            strokeWidth = 2.dp,
            modifier = modifier.size(16.dp),
        )
    } else {
        CircularProgressIndicator(
            color = color,
            strokeWidth = 2.dp,
            modifier = modifier.size(16.dp),
        )
    }
}

private const val REDUCED_MOTION_SPINNER_PROGRESS = 0.75f

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeToDismissToast(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value != DismissValue.Default) {
                onDismiss()
            }
            true
        },
    )
    SwipeToDismiss(
        state = dismissState,
        background = {},
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        content()
    }
}
