package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50

@Composable
internal fun SharingThemedSnackbar(
    snackbarData: SnackbarData,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) {
    val actionLabel = snackbarData.actionLabel
    val actionComposable: (@Composable () -> Unit)? = if (actionLabel != null) {
        @Composable {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = shareColors.snackbarText),
                onClick = { snackbarData.performAction() },
                content = { TextH50(actionLabel) },
            )
        }
    } else {
        null
    }
    Snackbar(
        modifier = modifier.padding(12.dp),
        content = { TextH50(snackbarData.message, color = shareColors.snackbarText) },
        action = actionComposable,
        actionOnNewLine = false,
        shape = MaterialTheme.shapes.small,
        backgroundColor = shareColors.snackbar,
        contentColor = shareColors.snackbar,
        elevation = 0.dp,
    )
}
