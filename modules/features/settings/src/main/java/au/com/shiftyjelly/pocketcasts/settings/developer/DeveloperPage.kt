package au.com.shiftyjelly.pocketcasts.settings.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import io.sentry.Sentry
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun DeveloperPage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onShowkaseClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onTriggerNotificationClick: () -> Unit,
    onDeleteFirstEpisodeClick: () -> Unit,
    onTriggerUpdateEpisodeDetails: () -> Unit
) {

    Column(modifier = modifier) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_developer),
            onNavigationClick = onBackClick,
        )
        ShowkaseSetting(onClick = onShowkaseClick)
        ForceRefreshSetting(onClick = onForceRefreshClick)
        SendCrashSetting()
        TriggerNotificationSetting(onClick = onTriggerNotificationClick)
        DeleteFirstEpisodeSetting(onClick = onDeleteFirstEpisodeClick)
        TriggerUpdateEpisodeDetails(onClick = onTriggerUpdateEpisodeDetails)
    }
}

@Composable
private fun ShowkaseSetting(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Showkase",
        secondaryText = "Compose components",
        icon = rememberVectorPainter(Icons.Outlined.HomeRepairService),
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
private fun ForceRefreshSetting(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Force refresh",
        secondaryText = "Refresh podcasts and sync data",
        icon = rememberVectorPainter(Icons.Default.Refresh),
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
private fun SendCrashSetting(
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Report a crash",
        secondaryText = "Send an exception to Sentry",
        icon = rememberVectorPainter(Icons.Outlined.BugReport),
        modifier = modifier.clickable {
            Sentry.captureException(Exception("Test crash"))
        }
    )
}

@Composable
private fun TriggerNotificationSetting(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Trigger new episode notification",
        secondaryText = "Test the notifications work",
        icon = rememberVectorPainter(Icons.Outlined.Notifications),
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
private fun DeleteFirstEpisodeSetting(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Delete first episodes",
        secondaryText = "Testing the podcast page can find missing episodes",
        icon = rememberVectorPainter(Icons.Outlined.Delete),
        modifier = modifier.clickable { onClick() }
    )
}

@Composable
private fun TriggerUpdateEpisodeDetails(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Trigger update episode details",
        secondaryText = "Test the update episode details task with 5 random episodes",
        icon = rememberVectorPainter(Icons.Outlined.Downloading),
        modifier = modifier.clickable { onClick() }
    )
}

@Preview(name = "Light")
@Composable
private fun DeveloperPageLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        DeveloperPagePreview()
    }
}

@Preview(name = "Dark")
@Composable
private fun DeveloperPageDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        DeveloperPagePreview()
    }
}

@Composable
private fun DeveloperPagePreview() {
    DeveloperPage(
        onBackClick = {},
        onShowkaseClick = {},
        onForceRefreshClick = {},
        onTriggerNotificationClick = {},
        onDeleteFirstEpisodeClick = {},
        onTriggerUpdateEpisodeDetails = {}
    )
}
