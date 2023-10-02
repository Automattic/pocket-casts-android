package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.foundation.Image
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun HorizontalLogoText(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val logoColor = if (MaterialTheme.theme.isLight) Color.Black else Color.White
    Image(
        painter = painterResource(IR.drawable.logo_pocket_casts),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(logoColor),
        modifier = modifier
    )
}

@ShowkaseComposable(name = "Logo", group = "Images", styleName = "HorizontalLogoText - Dark", defaultStyle = true)
@Preview(name = "Dark")
@Composable
fun HorizontalLogoTextDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        HorizontalLogoText()
    }
}

@ShowkaseComposable(name = "Logo", group = "Images", styleName = "HorizontalLogoText - Light")
@Preview(name = "Light")
@Composable
fun HorizontalLogoTextLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        HorizontalLogoText()
    }
}
