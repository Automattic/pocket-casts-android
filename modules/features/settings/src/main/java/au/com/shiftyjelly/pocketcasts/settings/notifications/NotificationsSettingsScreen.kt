package au.com.shiftyjelly.pocketcasts.settings.notifications

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.core.net.toUri
import androidx.fragment.compose.AndroidFragment
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.settings.notifications.components.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource

@Composable
internal fun NotificationsSettingsScreen(
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    viewModel: NotificationsSettingViewModel,
    onAdvancedSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: NotificationsSettingViewModel.State by viewModel.state.collectAsState()

    // Unfortunately, PodcastSelectFragment was meant to be used from another fragment that defines a toolbar.
    // This flag is used to determine whether we should render the podcast selector inside this composable and change toolbar title and override back navigation when necessary.
    var isShowingPodcastSelector by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data ->
            val ringtone: Uri? = getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            val filePath = ringtone?.toString().orEmpty()
            viewModel.onPreferenceChanged(
                // construct a fake item to pass the new value, otherwise I'd need to hold a reference to the original preference item
                NotificationPreference.ValueHolderPreference(
                    preference = NotificationPreferences.NEW_EPISODES_RINGTONE,
                    title = "",
                    value = filePath,
                    displayValue = ""
                )
            )
        }

    }

    CallOnce {
        viewModel.onShown()
    }

    Column {
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
                modifier
                    .background(MaterialTheme.theme.colors.primaryUi02)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                for (category in state.categories) {
                    item {
                        NotificationPreferenceCategory(
                            categoryTitle = category.title,
                            items = category.preferences,
                            onItemClicked = { preference ->
                                when (preference.preference) {
                                    NotificationPreferences.NEW_EPISODES_ADVANCED -> {
                                        onAdvancedSettingsClicked()
                                    }

                                    NotificationPreferences.NEW_EPISODES_CHOOSE_PODCASTS -> {
                                        isShowingPodcastSelector = true
                                    }

                                    NotificationPreferences.NEW_EPISODES_RINGTONE -> {
                                        launcher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                                            // Select "Silent" if empty
                                            runCatching {
                                                (preference.value as String).toUri()
                                            }.getOrNull()?.let {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                                            }
                                        })
                                    }

                                    else -> Unit
                                }
                                viewModel.onPreferenceChanged(preference)
                            },
                            modifier = Modifier.fillMaxWidth()
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