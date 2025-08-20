package au.com.shiftyjelly.pocketcasts.compose.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            color = color,
            modifier = Modifier
                .progressSemantics()
                .size(24.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun BookmarkRowDarkPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.DARK) {
        LoadingView()
    }
}

@Preview(name = "Light")
@Composable
private fun BookmarkRowLightPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.LIGHT) {
        LoadingView()
    }
}
