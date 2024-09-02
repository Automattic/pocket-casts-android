package au.com.shiftyjelly.pocketcasts.settings.notification

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.settings.notification.MediaActionsViewModel.State
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaActionsPage(
    bottomInset: Dp,
    state: State,
    onBackClick: () -> Unit,
    onShowCustomActionsChanged: (Boolean) -> Unit,
    onActionsOrderChanged: (List<MediaNotificationControls>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_media_actions_customise),
            onNavigationClick = onBackClick,
        )

        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val actions = state.actions.toMutableList().apply {
                // adjust for the two header items
                var toIndex = to.index - 2
                var fromIndex = from.index - 2
                // adjust for the sticky header after three items
                if (toIndex > 3) {
                    toIndex -= 1
                }
                if (fromIndex > 3) {
                    fromIndex -= 1
                }
                add(toIndex, removeAt(fromIndex))
            }
            onActionsOrderChanged(actions)
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = bottomInset),
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(vertical = 8.dp),
        ) {
            item {
                ShowCustomActionsSettings(
                    customActionsVisibility = state.customActionsVisibility,
                    onShowCustomActionsChanged = {
                        onShowCustomActionsChanged(it)
                    },
                )
            }
            item {
                SettingRow(
                    primaryText = stringResource(LR.string.settings_media_actions_prioritize_title),
                    secondaryText = stringResource(LR.string.settings_media_actions_prioritize_subtitle),
                    indent = false,
                )
            }

            state.actions.chunked(3).forEachIndexed { index, actions ->
                if (index == 1) {
                    stickyHeader {
                        TextP50(
                            text = stringResource(LR.string.settings_other_media_actions),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(actions, key = { it.key }) { control ->
                    ReorderableItem(reorderableLazyListState, key = control.key) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                        Surface(elevation = elevation) {
                            Row(modifier = modifier.fillMaxWidth()) {
                                Spacer(Modifier.width(24.dp))
                                Text(
                                    text = stringResource(control.controlName),
                                    color = MaterialTheme.theme.colors.primaryText01,
                                    modifier = Modifier.padding(16.dp),
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    modifier = Modifier.draggableHandle(),
                                    onClick = {},
                                ) {
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowCustomActionsSettings(
    customActionsVisibility: Boolean,
    onShowCustomActionsChanged: (Boolean) -> Unit,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_media_actions_show_title),
        secondaryText = stringResource(LR.string.settings_media_actions_show_subtitle),
        toggle = SettingRowToggle.Switch(checked = customActionsVisibility),
        indent = false,
        modifier = Modifier
            .padding(top = 8.dp)
            .toggleable(
                value = customActionsVisibility,
                role = Role.Switch,
            ) {
//                    analyticsTracker.track(
//                        AnalyticsEvent.SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_SHOW_CUSTOM_TOGGLED,
//                        mapOf("enabled" to it),
//                    )
                onShowCustomActionsChanged(it)
            },
    )
}
