package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme

@Composable
fun QueueButton(inUpNext: Boolean, tint: Color, onClick: () -> Unit) {
    val icon = if (inUpNext) {
        R.drawable.ic_upnext_remove
    } else {
        R.drawable.ic_upnext_playnext
    }

    Icon(
        painter = painterResource(icon),
        tint = tint,
        contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.podcasts_up_next),
        modifier = Modifier
            .size(24.dp)
            .clickable { onClick() }
    )
}

@Preview
@Composable
private fun Preview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        Column {
            QueueButton(
                inUpNext = true,
                tint = Color.Yellow,
                onClick = {}
            )
            QueueButton(
                inUpNext = false,
                tint = Color.Blue,
                onClick = {}
            )
        }
    }
}
