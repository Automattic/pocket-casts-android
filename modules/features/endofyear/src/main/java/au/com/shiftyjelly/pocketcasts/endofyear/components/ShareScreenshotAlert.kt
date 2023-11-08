package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.compose.components.DialogFrame
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import com.afollestad.materialdialogs.ModalDialog.onDismiss
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShareScreenshotAlert(
    onPositiveButtonClicked: () -> Unit,
    onNegativeButtonClicked: () -> Unit,
) {
    DialogFrame(
        title = stringResource(LR.string.end_of_year_share_story_alert_title),
        buttons = listOf(
            DialogButtonState(
                text = stringResource(LR.string.share),
                onClick = { onPositiveButtonClicked() }
            ),
            DialogButtonState(
                text = stringResource(LR.string.not_now),
                onClick = { onNegativeButtonClicked() }
            ),
        ),
        onDismissRequest = { onDismiss() },
        content = {
            TextP40(
                text = stringResource(LR.string.end_of_year_share_story_alert_description),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .padding(horizontal = 24.dp)
            )
        }
    )
}
