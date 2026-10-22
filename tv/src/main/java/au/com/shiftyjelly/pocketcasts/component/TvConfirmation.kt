package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ColumnScope.TvConfirmationContent(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Text(
        text = title,
        color = MaterialTheme.tvColors.textPrimary,
        style = MaterialTheme.tvTypography.headline.copy(textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = message,
        color = MaterialTheme.tvColors.textSecondary,
        style = MaterialTheme.tvTypography.caption1.copy(textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth(),
    )
    TvModalButton(
        text = confirmLabel,
        onClick = onConfirm,
    )
    TvModalButton(
        text = stringResource(LR.string.cancel),
        onClick = onCancel,
        modifier = Modifier.focusRequester(focusRequester),
    )
}

@Preview
@Composable
private fun TvConfirmationContentPreview() {
    TvTheme {
        TvModalSurface {
            TvConfirmationContent(
                title = "Mark as played?",
                message = "This will stop the current playback.",
                confirmLabel = "Mark as played",
                onConfirm = {},
                onCancel = {},
            )
        }
    }
}
