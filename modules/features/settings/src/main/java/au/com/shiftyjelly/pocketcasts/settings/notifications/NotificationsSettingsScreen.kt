package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.settings.notifications.components.NotificationPreferenceCategory

@Composable
internal fun NotificationsSettingsScreen(
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    viewModel: NotificationsSettingViewModel,
    modifier: Modifier = Modifier,
) {
    val state: NotificationsSettingViewModel.State by viewModel.state.collectAsState()

    CallOnce {
        viewModel.onShown()
    }

    Column {
        ThemedTopAppBar(
            title = stringResource(R.string.settings_title_notifications),
            bottomShadow = true,
            onNavigationClick = { onBackPressed() },
        )

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
                        onItemClicked = viewModel::onPreferenceClicked,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}