package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
        modifier = if (data.actionLabel != null) Modifier.heightIn(min = 56.dp) else Modifier,
    ) {
        Text(
            text = data.message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

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
