package au.com.shiftyjelly.pocketcasts.kids

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun KidsDialog(
    modifier: Modifier = Modifier,
    onSendFeedbackClick: () -> Unit,
    onNoThankYouClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxWidth(),
        ) {
            if (!isLandscape) {
                Image(
                    painterResource(IR.drawable.kids_face_with_background),
                    contentDescription = stringResource(LR.string.kids_profile_face_image),
                    modifier = modifier
                        .width(242.dp)
                        .height(160.dp)
                        .padding(bottom = 12.dp),
                )
            }

            TextH30(
                text = stringResource(LR.string.thank_you_for_your_interest),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W600,
                modifier = modifier.padding(bottom = 12.dp),
            )

            TextH50(
                text = stringResource(LR.string.kids_bottom_sheet_description),
                fontWeight = FontWeight.W500,
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(bottom = 12.dp),
            )

            RowButton(
                text = stringResource(LR.string.send_feedback),
                contentDescription = stringResource(LR.string.send_feedback),
                onClick = { onSendFeedbackClick() },
                includePadding = false,
                textColor = MaterialTheme.theme.colors.primaryInteractive02,
                modifier = modifier.padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                ),
            )

            RowOutlinedButton(
                text = stringResource(LR.string.no_thank_you),
                onClick = { onNoThankYouClick() },
                includePadding = false,
                modifier = modifier.padding(bottom = 12.dp),
                border = BorderStroke(2.dp, MaterialTheme.theme.colors.primaryInteractive01),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryInteractive01),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewKidsDialogDialog(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        KidsDialog(
            onSendFeedbackClick = {},
            onNoThankYouClick = {},
        )
    }
}
