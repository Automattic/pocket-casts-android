package au.com.shiftyjelly.pocketcasts.podcasts.view.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun EnableNotificationsPromptScreen(
    onCtaClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onDismissClicked,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_close),
                contentDescription = stringResource(LR.string.close),
                tint = MaterialTheme.theme.colors.primaryText01,
            )
        }
        Spacer(modifier = Modifier.height(42.dp))
        Image(
            painter = painterResource(IR.drawable.android_mockup),
            contentDescription = stringResource(LR.string.notification_mockup_image_description),
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextH10(
            modifier = Modifier.padding(horizontal = 22.dp),
            text = stringResource(LR.string.notification_prompt_title),
            color = MaterialTheme.theme.colors.primaryText01,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextP40(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(LR.string.notification_prompt_message),
            color = MaterialTheme.theme.colors.secondaryText02,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        RowButton(
            text = stringResource(LR.string.notification_prompt_cta),
            textColor = MaterialTheme.theme.colors.primaryInteractive02,
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            ),
            includePadding = false,
            onClick = onCtaClicked,
        )
    }
}

@Preview
@Composable
private fun EnableNotificationsPromptScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) = AppThemeWithBackground(themeType) {
    EnableNotificationsPromptScreen(
        onDismissClicked = {},
        onCtaClicked = {},
    )
}
