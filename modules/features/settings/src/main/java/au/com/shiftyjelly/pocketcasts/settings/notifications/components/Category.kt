package au.com.shiftyjelly.pocketcasts.settings.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference

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
                is NotificationPreference.LocalPreference.SwitchPreference -> SwitchPreferenceRow(
                    preference = item,
                    onValueChanged = { onItemClicked(item) }
                )
                else -> Text("To be implemented later")
            }
        }
    }
}

@Composable
internal fun SwitchPreferenceRow(
    preference: NotificationPreference.LocalPreference.SwitchPreference,
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
            )
        )
    }
}

@Preview
@Composable
private fun NotificationCategoryPreview() {
    MaterialTheme {
        var isChecked by remember { mutableStateOf(false) }
        NotificationCategory(
            categoryTitle = "Test Category",
            items = listOf(
                NotificationPreference.LocalPreference.SwitchPreference(
                    title = "item 1",
                    preferenceKey = "key1",
                    value = isChecked
                ),
            ),
            onItemClicked = { isChecked = !isChecked }
        )
    }
}