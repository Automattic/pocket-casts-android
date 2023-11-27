package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private val LogoHeight = 26.dp

@Composable
fun StoryAppLogo(modifier: Modifier = Modifier) {
    AppTheme(Theme.ThemeType.DARK) {
        HorizontalLogo(
            modifier = modifier.height(LogoHeight)
        )
    }
}
