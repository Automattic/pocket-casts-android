package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodcastSelectedText(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01
) {
    val selectedString = when (count) {
        0 -> stringResource(LR.string.settings_choose_podcasts)
        1 -> stringResource(LR.string.podcasts_chosen_singular)
        else -> stringResource(LR.string.podcasts_chosen_plural, count)
    }
    TextP40(
        text = selectedString,
        color = color,
        modifier = modifier
    )
}

@Preview(name = "PodcastSelectText in English", showBackground = true)
@Composable
fun PodcastSelectedTextEnglishPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Column(modifier = Modifier.padding(8.dp)) {
            PodcastSelectedText(count = 0, modifier = Modifier.padding(bottom = 8.dp))
            PodcastSelectedText(count = 1, modifier = Modifier.padding(bottom = 8.dp))
            PodcastSelectedText(count = 12)
        }
    }
}

@Preview(name = "PodcastSelectText in French", showBackground = true, locale = "fr")
@Composable
fun PodcastSelectedTextFrenchPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Column(modifier = Modifier.padding(8.dp)) {
            PodcastSelectedText(count = 0, modifier = Modifier.padding(bottom = 8.dp))
            PodcastSelectedText(count = 1, modifier = Modifier.padding(bottom = 8.dp))
            PodcastSelectedText(count = 12)
        }
    }
}
