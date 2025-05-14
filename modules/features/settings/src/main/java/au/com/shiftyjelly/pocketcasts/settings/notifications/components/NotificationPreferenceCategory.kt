package au.com.shiftyjelly.pocketcasts.settings.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.RadioGroupPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.TextPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
internal fun NotificationPreferenceCategory(
    categoryTitle: String,
    items: List<NotificationPreference<*>>,
    onItemClicked: (NotificationPreference<*>) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingSection(
        modifier = modifier,
        heading = categoryTitle
    ) {
        items.forEach { item ->
            when (item) {
                is SwitchPreference -> SettingRow(
                    primaryText = item.title,
                    toggle =SettingRowToggle.Switch(checked = item.value),
                    modifier = modifier.toggleable(
                        value = item.value,
                        role = Role.Switch,
                    ) { onItemClicked(item) },
                )

                is TextPreference -> SettingRow(
                    primaryText = item.title,
                    secondaryText = item.value,
                    modifier = modifier.clickable { onItemClicked(item) }
                )

                is RadioGroupPreference<*> -> SettingRow(
                    primaryText = item.title,
                    secondaryText = (item.value as? String).orEmpty(),
                )
            }
        }
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
                    title = "offItem",
                    value = false,
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                ),
                SwitchPreference(
                    title = "onItem",
                    value = true,
                    preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME,
                ),
                TextPreference(
                    title = "singleSelectItem",
                    value = "value",
                    preference = NotificationPreferences.NEW_EPISODES_ACTIONS
                ),
            ),
            onItemClicked = { }
        )
    }
}