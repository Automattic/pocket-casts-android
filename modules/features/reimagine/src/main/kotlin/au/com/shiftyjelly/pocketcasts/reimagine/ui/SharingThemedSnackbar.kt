package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.TextButton
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
    val actionLabel = snackbarData.visuals.actionLabel
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
        content = { TextH50(snackbarData.visuals.message, color = shareColors.snackbarText) },
        action = actionComposable,
        actionOnNewLine = false,
        shape = MaterialTheme.shapes.small,
        containerColor = shareColors.snackbar,
        contentColor = shareColors.snackbar,
    )
}
