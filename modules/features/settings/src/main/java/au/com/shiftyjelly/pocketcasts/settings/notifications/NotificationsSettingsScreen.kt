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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.fragment.compose.AndroidFragment
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.settings.notifications.components.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import kotlinx.coroutines.runBlocking

@Composable
internal fun NotificationsSettingsScreen(
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    viewModel: NotificationsSettingViewModel,
    onAdvancedSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: NotificationsSettingViewModel.State by viewModel.state.collectAsState()

    var isShowingPodcastSelector by remember { mutableStateOf(false) }

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

                                    else -> Unit
                                }
                                viewModel.onPreferenceClicked(preference)
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
                    onUpdate = {
                        it.listener = object : PodcastSelectFragment.Listener {
                            override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
                                viewModel.onSelectedPodcastsChanged(newSelection)
                            }

                            override fun podcastSelectFragmentGetCurrentSelection(): List<String> = runBlocking {
                                viewModel.getSelectedPodcastIds()
                            }
                        }
                    }
                )
            }
        }
    }
}