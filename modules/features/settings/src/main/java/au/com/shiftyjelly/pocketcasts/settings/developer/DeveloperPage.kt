package au.com.shiftyjelly.pocketcasts.settings.developer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun DeveloperPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onTriggerNotificationClick: () -> Unit,
    onDeleteFirstEpisodeClick: () -> Unit,
    onTriggerUpdateEpisodeDetails: () -> Unit,
    onTriggerResetEoYModalProfileBadge: () -> Unit,
    onSendCrash: (String) -> Unit,
    onShowWhatsNewClick: () -> Unit,
    onShowNotificationsTestingClick: () -> Unit,
    onResetSuggestedFoldersSuggestion: () -> Unit,
    onResetPlaylistsOnboarding: () -> Unit,
    onResetNotificationsPrompt: () -> Unit,
    onShowAppReviewPrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var openCrashMessageDialog by remember { mutableStateOf(false) }
    var crashMessage by remember { mutableStateOf("Test crash") }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomInset),
    ) {
        item {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_developer),
                onNavigationClick = onBackPress,
            )
        }
        item {
            ForceRefreshSetting(onClick = onForceRefreshClick)
        }
        item {
            SendCrashSetting(
                onClick = { onSendCrash(crashMessage) },
                onLongClick = { openCrashMessageDialog = true },
            )
        }
        item {
            TriggerNotificationSetting(onClick = onTriggerNotificationClick)
        }
        item {
            DeleteFirstEpisodeSetting(onClick = onDeleteFirstEpisodeClick)
        }
        item {
            TriggerUpdateEpisodeDetails(onClick = onTriggerUpdateEpisodeDetails)
        }
        item {
            EndOfYear(onClick = onTriggerResetEoYModalProfileBadge)
        }
        item {
            ResetSuggestedFoldersSuggestion(onClick = onResetSuggestedFoldersSuggestion)
        }
        item {
            ShowWhatsNew(onClick = onShowWhatsNewClick)
        }
        item {
            ShowAppReviewPrompt(onClick = onShowAppReviewPrompt)
        }
        item {
            NotificationsTesting(onClick = onShowNotificationsTestingClick)
        }
        item {
            ResetNotificationsPrompt(onClick = onResetNotificationsPrompt)
        }
        item {
            ResetPlaylistsOnboarding(onClick = onResetPlaylistsOnboarding)
        }
        item {
            CrashApp()
        }
    }

    if (openCrashMessageDialog) {
        CrashMessageDialog(
            initialMessage = crashMessage,
            onDismiss = { openCrashMessageDialog = false },
            onConfirm = { message ->
                openCrashMessageDialog = false
                crashMessage = message
            },
        )
    }
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
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SendCrashSetting(
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Report a crash",
        secondaryText = "Send an exception to Sentry",
        icon = rememberVectorPainter(Icons.Outlined.BugReport),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    )
}

@Composable
private fun CrashMessageDialog(
    initialMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf(initialMessage) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextH30(
                    text = "Use a custom crash message",
                    modifier = Modifier.padding(16.dp),
                )
                FormField(
                    value = message,
                    placeholder = "Crash message",
                    onValueChange = { message = it },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { onConfirm(message) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
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
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun ResetNotificationsPrompt(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Reset notifications prompt",
        secondaryText = "Show enable notifications prompt on Podcasts when the permission is missing",
        icon = rememberVectorPainter(Icons.Outlined.Notifications),
        modifier = modifier.clickable { onClick() },
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
        modifier = modifier.clickable { onClick() },
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
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun EndOfYear(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Reset End of Year modal",
        secondaryText = "Reset modal and profile badge for end of year",
        icon = rememberVectorPainter(Icons.Outlined.EditCalendar),
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun ShowWhatsNew(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Show What's New",
        secondaryText = "Open the What's New page",
        icon = rememberVectorPainter(Icons.Outlined.NewReleases),
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun ShowAppReviewPrompt(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Show app review prompt",
        secondaryText = "Open the prompt to give ratings",
        icon = rememberVectorPainter(Icons.Outlined.StarBorder),
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun NotificationsTesting(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Notifications testing",
        secondaryText = "Adjust delays and trigger notifications on-demand",
        icon = rememberVectorPainter(Icons.Outlined.Notifications),
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun ResetPlaylistsOnboarding(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Reset Playlists onboarding",
        secondaryText = "Show Playlists onboarding and tooltips",
        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.PlaylistPlay),
        modifier = modifier.clickable { onClick() },
    )
}

@Composable
private fun CrashApp(
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Go Bye Bye",
        secondaryText = "Crashes the app",
        icon = rememberVectorPainter(Icons.Outlined.ErrorOutline),
        modifier = modifier.clickable {
            throw RuntimeException("Crashing in 3, 2, 1… Boom!")
        },
    )
}

@Composable
private fun ResetSuggestedFoldersSuggestion(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Reset Smart Folders",
        secondaryText = "Allows to retrigger suggested folders",
        icon = rememberVectorPainter(Icons.Outlined.Folder),
        modifier = modifier.clickable { onClick() },
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
        onBackPress = {},
        onForceRefreshClick = {},
        onTriggerNotificationClick = {},
        onDeleteFirstEpisodeClick = {},
        onTriggerUpdateEpisodeDetails = {},
        onTriggerResetEoYModalProfileBadge = {},
        bottomInset = 0.dp,
        onSendCrash = {},
        onShowWhatsNewClick = {},
        onResetSuggestedFoldersSuggestion = {},
        onShowNotificationsTestingClick = {},
        onResetPlaylistsOnboarding = {},
        onResetNotificationsPrompt = {},
        onShowAppReviewPrompt = {},
    )
}

@Preview
@Composable
private fun CrashMessageDialogPreview() {
    CrashMessageDialog(
        initialMessage = "Test crash",
        onDismiss = {},
        onConfirm = {},
    )
}
