package au.com.shiftyjelly.pocketcasts.settings.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TrackingConsentDialog(
    onAllow: () -> Unit,
    onAskAppNotToTrack: () -> Unit,
) {
    Dialog(onDismissRequest = { onAskAppNotToTrack() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextP30(
                    text = stringResource(LR.string.tracking_consent_dialog_title),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                TextP50(
                    text = stringResource(LR.string.tracking_consent_dialog_message),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText02,
                )

                Spacer(Modifier.height(16.dp))

                RowOutlinedButton(
                    text = stringResource(LR.string.tracking_consent_dialog_reject),
                    includePadding = false,
                    maxLines = 1,
                    onClick = { onAskAppNotToTrack() },
                )

                Spacer(Modifier.height(8.dp))

                RowButton(
                    text = stringResource(LR.string.tracking_consent_dialog_allow),
                    includePadding = false,
                    onClick = { onAllow() },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingConsentDialogPreview() {
    TrackingConsentDialog(
        onAllow = {},
        onAskAppNotToTrack = {},
    )
}
