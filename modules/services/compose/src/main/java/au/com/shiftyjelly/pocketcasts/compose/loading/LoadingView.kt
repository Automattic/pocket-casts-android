package au.com.shiftyjelly.pocketcasts.compose.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun LoadingView() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .progressSemantics()
                .size(24.dp),
            strokeWidth = 2.dp,
        )
    }
}

@ShowkaseComposable(name = "Circular Loading Indicator", group = "Loading Indicator", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun BookmarkRowDarkPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.DARK) {
        LoadingView()
    }
}

@ShowkaseComposable(name = "Circular Loading Indicator", group = "Loading Indicator", styleName = "Light")
@Preview(name = "Light")
@Composable
fun BookmarkRowLightPreview() {
    AppThemeWithBackground(themeType = Theme.ThemeType.LIGHT) {
        LoadingView()
    }
}
