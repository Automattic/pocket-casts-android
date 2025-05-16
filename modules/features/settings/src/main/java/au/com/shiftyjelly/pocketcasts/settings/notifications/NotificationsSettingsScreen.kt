package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.settings.notifications.components.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory as CategoryModel

@Stable
class OnBackPressHandlerHolder(
    var onBackPress: () -> Boolean,
)

@Composable
internal fun NotificationsSettingsScreen(
    state: NotificationsSettingsViewModel.State,
    onPreferenceChanged: (NotificationPreferenceType) -> Unit,
    onAdvancedSettingsClicked: () -> Unit,
    onSelectRingtoneClicked: (String?) -> Unit,
    onBackPressHandlerHolder: OnBackPressHandlerHolder,
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    // Unfortunately, PodcastSelectFragment was meant to be used from another fragment that defines a toolbar.
    // This flag is used to determine whether we should render the podcast selector inside this composable and change toolbar title and override back navigation when necessary.
    var isShowingPodcastSelector by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isShowingPodcastSelector) {
        onBackPressHandlerHolder.onBackPress = {
            if (isShowingPodcastSelector) {
                isShowingPodcastSelector = false
                true
            } else {
                false
            }
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .fillMaxHeight(),
    ) {
        ThemedTopAppBar(
            title = stringResource(if (isShowingPodcastSelector) R.string.settings_select_podcasts else R.string.settings_title_notifications),
            bottomShadow = true,
            onNavigationClick = {
                if (isShowingPodcastSelector) {
                    isShowingPodcastSelector = false
                } else {
                    onBackPressed()
                }
            },
        )

        Box {
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                for (category in state.categories) {
                    item {
                        NotificationPreferenceCategory(
                            categoryTitle = category.title.asString(),
                            items = category.preferences,
                            onItemClicked = { preference ->
                                when (preference) {
                                    is NotificationPreferenceType.AdvancedSettings -> {
                                        onAdvancedSettingsClicked()
                                    }

                                    is NotificationPreferenceType.NotifyOnThesePodcasts -> {
                                        isShowingPodcastSelector = true
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
            if (isShowingPodcastSelector) {
                AndroidFragment(
                    modifier = Modifier.fillMaxSize(),
                    clazz = PodcastSelectFragment::class.java,
                    arguments = PodcastSelectFragment.createArgs(source = PodcastSelectFragmentSource.NOTIFICATIONS),
                )
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
            onBackPressHandlerHolder = OnBackPressHandlerHolder { false },
        )
    }
