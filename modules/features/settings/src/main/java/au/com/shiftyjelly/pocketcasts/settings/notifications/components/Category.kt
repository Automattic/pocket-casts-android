package au.com.shiftyjelly.pocketcasts.settings.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.ExternalPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.ValuePreference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference.ValuePreference.TextPreference
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
internal fun NotificationCategory(
    categoryTitle: String,
    items: List<NotificationPreference>,
    onItemClicked: (NotificationPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
    ) {
        TextH60(
            text = categoryTitle,
            color = MaterialTheme.theme.colors.primaryInteractive01
        )
        Spacer(modifier = Modifier.height(8.dp))
        items.forEach { item ->
            when (item) {
                is SwitchPreference -> SwitchPreferenceRow(
                    modifier = Modifier.fillMaxWidth(),
                    preference = item,
                    onValueChanged = { onItemClicked(item) }
                )

                is TextPreference -> TextPreferenceRow(
                    modifier = Modifier.fillMaxWidth(),
                    title = item.title,
                    subTitle = item.value,
                    onClicked = { onItemClicked(item) }
                )

                is ExternalPreference -> TextPreferenceRow(
                    modifier = Modifier.fillMaxWidth(),
                    title = item.title,
                    subTitle = item.description,
                    onClicked = { onItemClicked(item) }
                )
            }
        }
    }
}

@Composable
internal fun TextPreferenceRow(
    title: String,
    subTitle: String,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClicked() }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextP50(
            text = title,
            color = MaterialTheme.theme.colors.primaryText01,
        )
        TextP60(
            text = subTitle,
            color = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Composable
internal fun SwitchPreferenceRow(
    preference: SwitchPreference,
    onValueChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable {
            onValueChanged(!preference.value)
        },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextP50(
            text = preference.title,
            color = MaterialTheme.theme.colors.primaryText01
        )
        Switch(
            checked = preference.value,
            onCheckedChange = onValueChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.theme.colors.primaryInteractive01,
                checkedTrackColor = MaterialTheme.theme.colors.primaryInteractive01,
                uncheckedTrackColor = MaterialTheme.theme.colors.secondaryInteractive01Active,
                uncheckedThumbColor = MaterialTheme.theme.colors.secondaryInteractive01Active,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationCategoryPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        NotificationCategory(
            categoryTitle = "Test Category",
            items = listOf(
                SwitchPreference(
                    title = "offItem",
                    preferenceKey = "",
                    value = false,
                ),
                SwitchPreference(
                    title = "onItem",
                    preferenceKey = "",
                    value = true,
                ),
                TextPreference.SingleSelectPreference(
                    title = "singleSelectItem",
                    value = "value",
                    preferenceKey = ""
                ),
                TextPreference.MultiSelectPreference(
                    title = "multiSelectItem",
                    value = "value",
                    preferenceKey = ""
                ),
                ExternalPreference(
                    title = "externalItem",
                    description = "description"
                )
            ),
            onItemClicked = { }
        )
    }
}