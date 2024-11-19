package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.compose.components.DialogFrame
import au.com.shiftyjelly.pocketcasts.compose.components.DialogText
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ScreenshotDetectedDialog(
    onNotNow: () -> Unit,
    onShare: () -> Unit,
) {
    DialogFrame(
        onDismissRequest = onNotNow,
        title = stringResource(LR.string.end_of_year_share_dialog_title),
        content = {
            DialogText(stringResource(LR.string.end_of_year_share_dialog_message))
        },
        buttons = listOf(
            DialogButtonState(
                text = stringResource(LR.string.not_now),
                onClick = onNotNow,
            ),
            DialogButtonState(
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
