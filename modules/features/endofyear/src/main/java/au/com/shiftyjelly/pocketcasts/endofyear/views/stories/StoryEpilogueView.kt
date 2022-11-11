package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun StoryEpilogueView(
    story: StoryEpilogue,
    onReplayClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextH30(
            text = "Thank you for letting Pocket Casts be a part of your listening experience in 2022",
            color = story.tintColor,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(16.dp)
        )
        TextH30(
            text = "Don't forget to share with your friends and give a shout out to your favorite podcasts creators",
            color = story.tintColor,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(16.dp)
        )
        ReplayButton(onClick = onReplayClicked)
    }
}

@Composable
private fun ReplayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onClick() },
        colors = ButtonDefaults
            .outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
    ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = "")
        Text(
            text = stringResource(id = R.string.end_of_year_replay),
            fontSize = 18.sp,
            modifier = modifier.padding(2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StoryEpiloguePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            StoryEpilogueView(
                StoryEpilogue(),
                onReplayClicked = {}
            )
        }
    }
}
