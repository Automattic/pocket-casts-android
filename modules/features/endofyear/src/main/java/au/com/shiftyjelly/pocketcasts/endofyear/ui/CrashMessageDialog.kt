package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonProperties
import au.com.shiftyjelly.pocketcasts.compose.components.SimpleDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ScreenshotDetectedDialog(
    onNotNow: () -> Unit,
    onShare: () -> Unit,
) {
    SimpleDialog(
        onDismissRequest = onNotNow,
        title = stringResource(LR.string.end_of_year_share_dialog_title),
        body = stringResource(LR.string.end_of_year_share_dialog_message),
        buttonProperties = listOf(
            DialogButtonProperties(
                text = stringResource(LR.string.not_now),
                onClick = onNotNow,
            ),
            DialogButtonProperties(
                text = stringResource(LR.string.share),
                onClick = onShare,
            ),
        ),
    )
}

@Preview
@Composable
private fun CrashMessageDialogPreview() {
    ScreenshotDetectedDialog(
        onNotNow = {},
        onShare = {},
    )
}
