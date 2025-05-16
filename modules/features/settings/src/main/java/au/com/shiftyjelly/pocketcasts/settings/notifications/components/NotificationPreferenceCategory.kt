package au.com.shiftyjelly.pocketcasts.settings.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.MultiChoiceListener
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.updateListItemsMultiChoice
import okhttp3.internal.toImmutableList

@Suppress("UNCHECKED_CAST")
@Composable
internal fun NotificationPreferenceCategory(
    categoryTitle: String,
    items: List<NotificationPreferenceType>,
    onItemClicked: (NotificationPreferenceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingSection(
        modifier = modifier,
        heading = categoryTitle,
    ) {
        items.forEach { item ->
            when (item) {
                is NotificationPreferenceType.NotifyMeOnNewEpisodes -> {
                    SettingRow(
                        primaryText = item.title,
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled),
                        modifier = modifier.toggleable(
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClicked(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }
                is NotificationPreferenceType.HidePlaybackNotificationOnPause -> {
                    SettingRow(
                        primaryText = item.title,
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled),
                        modifier = modifier.toggleable(
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClicked(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.NotifyOnThesePodcasts -> {
                    SettingRow(
                        primaryText = item.title,
                        secondaryText = item.displayValue,
                        modifier = modifier.clickable { onItemClicked(item) },
                    )
                }

                is NotificationPreferenceType.AdvancedSettings -> {
                    SettingRow(
                        primaryText = item.title,
                        secondaryText = item.description,
                        modifier = modifier.clickable { onItemClicked(item) },
                    )
                }

                is NotificationPreferenceType.NotificationSoundPreference -> {
                    SettingRow(
                        primaryText = item.title,
                        secondaryText = item.displayedSoundName,
                        modifier = modifier.clickable { onItemClicked(item) },
                    )
                }

                is NotificationPreferenceType.NotificationVibration -> {
                    val context = LocalContext.current
                    SettingRadioDialogRow(
                        primaryText = item.title,
                        secondaryText = item.displayValue,
                        options = item.options,
                        savedOption = item.value,
                        optionToLocalisedString = {
                            context.getString(it.summary)
                        },
                        onSave = { value ->
                            onItemClicked(
                                item.copy(value = value),
                            )
                        },
                    )
                }

                is NotificationPreferenceType.PlayOverNotifications -> {
                    val context = LocalContext.current
                    SettingRadioDialogRow(
                        primaryText = item.title,
                        secondaryText = item.displayValue,
                        options = item.options,
                        savedOption = item.value,
                        optionToLocalisedString = {
                            context.getString(it.titleRes)
                        },
                        onSave = { value ->
                            onItemClicked(
                                item.copy(value = value),
                            )
                        },
                    )
                }

                is NotificationPreferenceType.NotificationActions -> {
                    val activity = LocalContext.current
                    SettingRow(
                        primaryText = item.title,
                        secondaryText = item.displayValue,
                        modifier = modifier.clickable {
                            val initialActions = item.value
                            val selectedActions = initialActions.toMutableList()
                            val initialSelection = selectedActions.map(NewEpisodeNotificationAction::ordinal).toIntArray()
                            val onSelect: MultiChoiceListener = { dialog, _, items ->
                                selectedActions.clear()
                                selectedActions.addAll(NewEpisodeNotificationAction.fromLabels(items.map { it.toString() }, activity.resources))
                                changeActionsDialog(3, selectedActions, dialog)
                            }
                            val dialog = MaterialDialog(activity)
                                .listItemsMultiChoice(
                                    items = NewEpisodeNotificationAction.labels(activity.resources),
                                    waitForPositiveButton = false,
                                    allowEmptySelection = true,
                                    initialSelection = initialSelection,
                                    selection = onSelect,
                                )
                                .show {
                                    title(res = R.string.settings_notification_actions_title)
                                    positiveButton(
                                        res = R.string.ok,
                                        click = {
                                            onItemClicked(
                                                item.copy(value = selectedActions.toImmutableList()),
                                            )
                                        },
                                    )
                                    negativeButton(res = R.string.cancel)
                                }
                            changeActionsDialog(3, selectedActions, dialog)
                        },
                    )
                }
            }
        }
    }
}

private fun changeActionsDialog(numberOfActions: Int, actions: MutableList<NewEpisodeNotificationAction>, dialog: MaterialDialog) {
    val resources = dialog.context.resources
    val onSelect: MultiChoiceListener = { dialogSelected, _, items ->
        actions.clear()
        actions.addAll(NewEpisodeNotificationAction.fromLabels(items.map { it.toString() }, resources))
        changeActionsDialog(numberOfActions, actions, dialogSelected)
    }

    if (actions.size < numberOfActions) {
        dialog.updateListItemsMultiChoice(items = NewEpisodeNotificationAction.labels(resources), disabledIndices = intArrayOf(), selection = onSelect)
    } else {
        val disabled = NewEpisodeNotificationAction.entries.filter { it !in actions }.map(NewEpisodeNotificationAction::ordinal)
        dialog.updateListItemsMultiChoice(items = NewEpisodeNotificationAction.labels(resources), disabledIndices = disabled.toIntArray(), selection = onSelect)
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationCategoryPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        NotificationPreferenceCategory(
            categoryTitle = "Test Category",
            items = listOf(
                NotificationPreferenceType.HidePlaybackNotificationOnPause(
                    title = "text",
                    isEnabled = true,
                ),
                NotificationPreferenceType.PlayOverNotifications(
                    title = "item 2",
                    value = PlayOverNotificationSetting.DUCK,
                    displayValue = "duck",
                    options = emptyList(),
                ),
            ),
            onItemClicked = { },
        )
    }
}
