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
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.MultiSelectPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.RadioGroupPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.TextPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.ValueHolderPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
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
    items: List<NotificationPreference<*>>,
    onItemClicked: (NotificationPreference<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingSection(
        modifier = modifier,
        heading = categoryTitle,
    ) {
        items.forEach { item ->
            when (item) {
                is SwitchPreference -> {
                    SettingRow(
                        primaryText = item.title,
                        toggle = SettingRowToggle.Switch(checked = item.value),
                        modifier = modifier.toggleable(
                            value = item.value,
                            role = Role.Switch,
                        ) { onItemClicked(item.copy(value = !item.value)) },
                    )
                }

                is TextPreference -> SettingRow(
                    primaryText = item.title,
                    secondaryText = item.value,
                    modifier = modifier.clickable { onItemClicked(item) },
                )

                is ValueHolderPreference<*> -> SettingRow(
                    primaryText = item.title,
                    secondaryText = item.displayValue,
                    modifier = modifier.clickable { onItemClicked(item) },
                )

                is RadioGroupPreference<*> -> {
                    val context = LocalContext.current
                    when (item.preference) {
                        NotificationPreferences.SETTINGS_PLAY_OVER -> {
                            val castedItem = item as RadioGroupPreference<PlayOverNotificationSetting>
                            SettingRadioDialogRow(
                                primaryText = item.title,
                                secondaryText = item.displayText,
                                options = castedItem.options,
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

                        NotificationPreferences.NEW_EPISODES_VIBRATION -> {
                            val castedItem = item as RadioGroupPreference<NotificationVibrateSetting>
                            SettingRadioDialogRow(
                                primaryText = item.title,
                                secondaryText = item.displayText,
                                options = castedItem.options,
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

                        else -> Unit
                    }
                }

                is MultiSelectPreference<*> -> {
                    val activity = LocalContext.current
                    SettingRow(
                        primaryText = item.title,
                        secondaryText = item.displayText,
                        modifier = modifier.clickable {
                            val initialActions = item.value.filterIsInstance<NewEpisodeNotificationAction>()
                            val selectedActions = initialActions.toMutableList()
                            val initialSelection = selectedActions.map(NewEpisodeNotificationAction::ordinal).toIntArray()
                            val onSelect: MultiChoiceListener = { dialog, _, items ->
                                selectedActions.clear()
                                selectedActions.addAll(NewEpisodeNotificationAction.fromLabels(items.map { it.toString() }, activity.resources))
                                changeActionsDialog(item.maxNumberOfSelectableOptions, selectedActions, dialog)
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
                                                (item as MultiSelectPreference<NewEpisodeNotificationAction>).copy(value = selectedActions.toImmutableList()),
                                            )
                                        },
                                    )
                                    negativeButton(res = R.string.cancel)
                                }
                            changeActionsDialog(item.maxNumberOfSelectableOptions, selectedActions, dialog)
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
                SwitchPreference(
                    title = "Off item",
                    value = false,
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                ),
                SwitchPreference(
                    title = "On item",
                    value = true,
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                ),
                TextPreference(
                    title = "Text item",
                    value = "Text value",
                    preference = NotificationPreferences.NEW_EPISODES_ACTIONS,
                ),
                ValueHolderPreference(
                    title = "Pi Value Holder item",
                    value = 3.14,
                    displayValue = "Pi",
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                ),
                RadioGroupPreference(
                    title = "Radio item",
                    value = 1,
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                    options = (1..5).toList(),
                    displayText = "one",
                ),
                MultiSelectPreference(
                    title = "Multiselect item",
                    value = (1..3).toList(),
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                    options = (1..10).toList(),
                    displayText = (1..3).joinToString(", "),
                    maxNumberOfSelectableOptions = 3,
                ),
            ),
            onItemClicked = { },
        )
    }
}
