package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun DividerLine() {
    Box(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Divider(
            color = MaterialTheme.theme.colors.primaryUi05,
            thickness = 1.dp,
        )
    }
}

@Preview
@Composable
fun DividerLinePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(modifier = Modifier.padding(16.dp)) {
            DividerLine()
        }
    }
}
