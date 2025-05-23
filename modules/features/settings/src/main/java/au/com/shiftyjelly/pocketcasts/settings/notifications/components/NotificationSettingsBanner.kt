package au.com.shiftyjelly.pocketcasts.settings.notifications.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun NotificationSettingsBanner(
    onSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.theme.colors.primaryUi02Active,
                shape = RoundedCornerShape(8.dp),
            )
            .clip(RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            contentDescription = "Notification icon",
            painter = painterResource(IR.drawable.ic_notifications),
            tint = MaterialTheme.theme.colors.primaryIcon03,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextH40(
                text = stringResource(LR.string.notifications_settings_turn_on_push_title),
                color = MaterialTheme.theme.colors.primaryText01,
            )
            TextH70(
                text = stringResource(LR.string.notifications_settings_turn_on_push_message),
                color = MaterialTheme.theme.colors.primaryText02,
            )
            TextH60(
                modifier = Modifier.clickable { onSettingsClicked() },
                text = stringResource(LR.string.notifications_settings_turn_on_push_button),
                color = MaterialTheme.theme.colors.primaryText02Selected,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNotificationSettingsBanner(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        NotificationSettingsBanner(
            onSettingsClicked = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
