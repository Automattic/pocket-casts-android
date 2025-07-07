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
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.MultiChoiceListener
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.updateListItemsMultiChoice
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun NotificationPreferenceCategory(
    categoryTitle: String,
    items: List<NotificationPreferenceType>,
    onItemClick: (NotificationPreferenceType) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    SettingSection(
        modifier = modifier,
        heading = categoryTitle,
    ) {
        items.forEach { item ->
            when (item) {
                is NotificationPreferenceType.NotifyMeOnNewEpisodes -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled, enabled = isEnabled),
                        modifier = Modifier.toggleable(
                            enabled = isEnabled,
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClick(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.EnableDailyReminders -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled, enabled = isEnabled),
                        modifier = Modifier.toggleable(
                            enabled = isEnabled,
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClick(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.EnableRecommendations -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled, enabled = isEnabled),
                        modifier = Modifier.toggleable(
                            enabled = isEnabled,
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClick(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.EnableNewFeaturesAndTips -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled, enabled = isEnabled),
                        modifier = Modifier.toggleable(
                            enabled = isEnabled,
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClick(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.EnableOffers -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled, enabled = isEnabled),
                        modifier = Modifier.toggleable(
                            enabled = isEnabled,
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClick(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.HidePlaybackNotificationOnPause -> {
                    SettingRow(
                        primaryText = item.title.asString(),
                        toggle = SettingRowToggle.Switch(checked = item.isEnabled),
                        modifier = Modifier.toggleable(
                            value = item.isEnabled,
                            role = Role.Switch,
                        ) { onItemClick(item.copy(isEnabled = !item.isEnabled)) },
                    )
                }

                is NotificationPreferenceType.NotifyOnThesePodcasts -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.displayValue.asString(),
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.AdvancedSettings -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.description.asString(),
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.DailyReminderSettings -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.description.asString(),
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.RecommendationSettings -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.description.asString(),
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.NewFeaturesAndTipsSettings -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.description.asString(),
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.OffersSettings -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.description.asString(),
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.NotificationSoundPreference -> {
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.displayedSoundName,
                        modifier = Modifier.clickable(enabled = isEnabled) { onItemClick(item) },
                    )
                }

                is NotificationPreferenceType.NotificationVibration -> {
                    val context = LocalContext.current
                    SettingRadioDialogRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.displayValue.asString(),
                        options = item.options,
                        savedOption = item.value,
                        optionToLocalisedString = {
                            context.getString(it.summary)
                        },
                        onSave = { value ->
                            onItemClick(
                                item.copy(value = value),
                            )
                        },
                    )
                }

                is NotificationPreferenceType.PlayOverNotifications -> {
                    val context = LocalContext.current
                    SettingRadioDialogRow(
                        primaryText = item.title.asString(),
                        secondaryText = item.displayValue.asString(),
                        options = item.options,
                        savedOption = item.value,
                        optionToLocalisedString = {
                            context.getString(it.titleRes)
                        },
                        onSave = { value ->
                            onItemClick(
                                item.copy(value = value),
                            )
                        },
                    )
                }

                is NotificationPreferenceType.NotificationActions -> {
                    val context = LocalContext.current
                    SettingRow(
                        enabled = isEnabled,
                        primaryText = item.title.asString(),
                        secondaryText = item.displayValue,
                        modifier = Modifier.clickable(enabled = isEnabled) {
                            val initialActions = item.value
                            val selectedActions = initialActions.toMutableList()
                            val initialSelection = selectedActions.map(NewEpisodeNotificationAction::ordinal).toIntArray()
                            val onSelect: MultiChoiceListener = { dialog, _, items ->
                                selectedActions.clear()
                                selectedActions.addAll(NewEpisodeNotificationAction.fromLabels(items.map { it.toString() }, context.resources))
                                changeActionsDialog(3, selectedActions, dialog)
                            }
                            val dialog = MaterialDialog(context)
                                .listItemsMultiChoice(
                                    items = NewEpisodeNotificationAction.labels(context.resources),
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
                                            onItemClick(
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
                    title = TextResource.fromText("text"),
                    isEnabled = true,
                ),
                NotificationPreferenceType.PlayOverNotifications(
                    title = TextResource.fromText("item 2"),
                    value = PlayOverNotificationSetting.DUCK,
                    displayValue = TextResource.fromText("duck"),
                    options = emptyList(),
                ),
            ),
            onItemClick = { },
        )
    }
}
