package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp
) {
    Divider(
        modifier = modifier,
        color = MaterialTheme.theme.colors.primaryUi05,
        thickness = 1.dp,
        startIndent = startIndent
    )
}

@Preview(showBackground = true)
@Composable
private fun HorizontalDividerLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Divider()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HorizontalDividerDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Divider()
        }
    }
}
