package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryIntro
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun StoryIntroView(
    story: StoryIntro,
    modifier: Modifier = Modifier,
) {
    TextH30(
        text = "Let's celebrate your year of listening..",
        color = story.tintColor,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun StoryIntroPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            StoryIntroView(
                StoryIntro(),
            )
        }
    }
}
