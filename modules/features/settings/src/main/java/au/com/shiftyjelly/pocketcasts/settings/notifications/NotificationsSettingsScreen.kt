package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.settings.notifications.components.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.components.NotificationSettingsBanner
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory as CategoryModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NotificationsSettingsScreen(
    state: NotificationsSettingsViewModel.State,
    onPreferenceChanged: (NotificationPreferenceType) -> Unit,
    onAdvancedSettingsClicked: (type: NotificationPreferenceType) -> Unit,
    onSelectRingtoneClicked: (String?) -> Unit,
    onSelectPodcastsClicked: () -> Unit,
    onSystemNotificationsSettingsClicked: () -> Unit,
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .fillMaxHeight(),
    ) {
        ThemedTopAppBar(
            title = stringResource(R.string.settings_title_notifications),
            bottomShadow = true,
            onNavigationClick = onBackPressed,
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            if (!state.areSystemNotificationsEnabled) {
                stickyHeader {
                    NotificationSettingsBanner(
                        onSettingsClicked = onSystemNotificationsSettingsClicked,
                        modifier = Modifier.fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            for (category in state.categories) {
                item {
                    NotificationPreferenceCategory(
                        isEnabled = state.areSystemNotificationsEnabled,
                        categoryTitle = category.title.asString(),
                        items = category.preferences,
                        onItemClicked = { preference ->
                            when (preference) {
                                is NotificationPreferenceType.AdvancedSettings,
                                is NotificationPreferenceType.DailyReminderSettings,
                                is NotificationPreferenceType.RecommendationSettings,
                                    -> {
                                    onAdvancedSettingsClicked(preference)
                                }

                                is NotificationPreferenceType.NotifyOnThesePodcasts -> {
                                    onSelectPodcastsClicked()
                                }

                                is NotificationPreferenceType.NotificationSoundPreference -> {
                                    onSelectRingtoneClicked(preference.notificationSound.path)
                                }

                                else -> Unit
                            }
                            onPreferenceChanged(preference)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewNotificationSettingsScreen(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) =
    AppTheme(themeType) {
        NotificationsSettingsScreen(
            state = NotificationsSettingsViewModel.State(
                areSystemNotificationsEnabled = true,
                categories = listOf(
                    CategoryModel(
                        title = TextResource.fromText("My episodes"),
                        preferences = listOf(
                            NotificationPreferenceType.NotifyMeOnNewEpisodes(
                                title = TextResource.fromText("Notify me"),
                                isEnabled = false,
                            ),
                        ),
                    ),
                    CategoryModel(
                        title = TextResource.fromText("Settings"),
                        preferences = listOf(
                            NotificationPreferenceType.PlayOverNotifications(
                                title = TextResource.fromText("Play over notifications"),
                                value = PlayOverNotificationSetting.DUCK,
                                displayValue = TextResource.fromText("Duck"),
                                options = emptyList(),
                            ),
                            NotificationPreferenceType.HidePlaybackNotificationOnPause(
                                title = TextResource.fromText("Hide playback notification on pause"),
                                isEnabled = true,
                            ),
                        ),
                    ),
                ),
            ),
            onPreferenceChanged = {},
            onAdvancedSettingsClicked = {},
            onBackPressed = {},
            bottomInset = 0.dp,
            onSelectRingtoneClicked = {},
            onSelectPodcastsClicked = {},
            onSystemNotificationsSettingsClicked = {}
        )
    }


@Preview
@Composable
private fun PreviewDisabledNotificationSettingsScreen(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) =
    AppTheme(themeType) {
        NotificationsSettingsScreen(
            state = NotificationsSettingsViewModel.State(
                areSystemNotificationsEnabled = false,
                categories = listOf(
                    CategoryModel(
                        title = TextResource.fromText("My episodes"),
                        preferences = listOf(
                            NotificationPreferenceType.NotifyMeOnNewEpisodes(
                                title = TextResource.fromText("Notify me"),
                                isEnabled = false,
                            ),
                        ),
                    ),
                    CategoryModel(
                        title = TextResource.fromText("Settings"),
                        preferences = listOf(
                            NotificationPreferenceType.PlayOverNotifications(
                                title = TextResource.fromText("Play over notifications"),
                                value = PlayOverNotificationSetting.DUCK,
                                displayValue = TextResource.fromText("Duck"),
                                options = emptyList(),
                            ),
                            NotificationPreferenceType.HidePlaybackNotificationOnPause(
                                title = TextResource.fromText("Hide playback notification on pause"),
                                isEnabled = true,
                            ),
                        ),
                    ),
                ),
            ),
            onPreferenceChanged = {},
            onAdvancedSettingsClicked = {},
            onBackPressed = {},
            bottomInset = 0.dp,
            onSelectRingtoneClicked = {},
            onSelectPodcastsClicked = {},
            onSystemNotificationsSettingsClicked = {}
        )
    }
