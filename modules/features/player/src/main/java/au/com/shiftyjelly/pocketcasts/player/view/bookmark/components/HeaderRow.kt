package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun HeaderRow(
    title: String,
    onOptionsMenuClicked: () -> Unit,
    style: HeaderRowColors,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        TextH40(
            text = title,
            color = style.textColor(),
        )
        IconButton(
            onClick = { onOptionsMenuClicked() },
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_more_vert_black_24dp),
                contentDescription = stringResource(LR.string.more_options),
                tint = style.iconColor(),
            )
        }
    }
}

sealed class HeaderRowColors {
    @Composable
    abstract fun textColor(): Color

    @Composable
    abstract fun iconColor(): Color

    object Default : HeaderRowColors() {
        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.primaryText02

        @Composable
        override fun iconColor(): Color = MaterialTheme.theme.colors.primaryIcon02
    }

    object Player : HeaderRowColors() {
        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.playerContrast02

        @Composable
        override fun iconColor(): Color = MaterialTheme.theme.colors.playerContrast01
    }
}

@ShowkaseComposable(name = "HeaderRow", group = "Bookmark", styleName = "Default - Light")
@Preview(name = "Light")
@Composable
fun HeaderRowDefaultLightPreview() {
    HeaderRowDefaultPreview(Theme.ThemeType.LIGHT)
}

@ShowkaseComposable(name = "HeaderRow", group = "Bookmark", styleName = "Default - Dark")
@Preview(name = "Dark")
@Composable
fun HeaderRowDefaultDarkPreview() {
    HeaderRowDefaultPreview(Theme.ThemeType.DARK)
}

@ShowkaseComposable(name = "HeaderRow", group = "Bookmark", styleName = "Default - Rose")
@Preview(name = "Rose")
@Composable
fun HeaderRowDefaultRosePreview() {
    HeaderRowDefaultPreview(Theme.ThemeType.ROSE)
}

@Composable
private fun HeaderRowDefaultPreview(themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        HeaderRow(
            title = "Header Row",
            onOptionsMenuClicked = {},
            style = HeaderRowColors.Default,
        )
    }
}

@ShowkaseComposable(name = "HeaderRow", group = "Bookmark", styleName = "Player")
@Preview(name = "Dark")
@Composable
fun HeaderRowPlayerPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        HeaderRow(
            title = "Header Row",
            onOptionsMenuClicked = {},
            style = HeaderRowColors.Player,
        )
    }
}
