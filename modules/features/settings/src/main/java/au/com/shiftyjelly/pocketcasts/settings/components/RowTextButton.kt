package au.com.shiftyjelly.pocketcasts.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
fun RowTextButton(text: String, modifier: Modifier = Modifier, secondaryText: String? = null, onClick: () -> Unit, fontSize: TextUnit = 17.sp) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(all = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = MaterialTheme.theme.colors.primaryText01
        )
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                fontSize = fontSize,
                color = MaterialTheme.theme.colors.primaryText02
            )
        }
    }
}
