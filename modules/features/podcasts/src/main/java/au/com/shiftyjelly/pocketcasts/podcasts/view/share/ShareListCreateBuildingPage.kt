package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute.uuid
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShareListCreateBuildingPage(
    onCloseClick: () -> Unit,
    viewModel: ShareListCreateViewModel,
    modifier: Modifier = Modifier
) {
    val state: ShareListCreateViewModel.State by viewModel.state.collectAsState()

    Column(modifier = modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
        ThemedTopAppBar(
            title = stringResource(LR.string.podcasts_share_creating_list),
            navigationButton = NavigationButton.Close,
            onNavigationClick = onCloseClick
        )

        CreateBuildingContent(title = state.title, podcasts = state.selectedPodcasts)
    }
}

@Composable
private fun CreateBuildingContent(title: String, podcasts: List<Podcast>, modifier: Modifier = Modifier) {
    var progress by remember { mutableStateOf(0f) }
    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 5000, easing = FastOutSlowInEasing)
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextH30(title)
            Spacer(Modifier.height(32.dp))
            Box {
                if (podcasts.size > 2) {
                    SharePodcastImage(podcast = podcasts[2], modifier = Modifier.rotate(-2f))
                }
                if (podcasts.size > 1) {
                    SharePodcastImage(podcast = podcasts[1], modifier = Modifier.rotate(-7f))
                }
                SharePodcastImage(podcast = podcasts[0], modifier = Modifier.rotate(7f))
            }
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(progress = progressAnimation)
        }
    }

    LaunchedEffect(Unit) {
        progress = 0.9f
    }
}

@Composable
private fun SharePodcastImage(podcast: Podcast, modifier: Modifier = Modifier) {
    PodcastImage(uuid = podcast.uuid, modifier = modifier.size(100.dp))
}

@Preview(showBackground = true)
@Composable
private fun CreateBuildingContentPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Column(Modifier.background(MaterialTheme.theme.colors.primaryUi03)) {
            CreateBuildingContent(
                title = "Top News Podcasts",
                podcasts = listOf(Podcast(), Podcast(), Podcast())
            )
        }
    }
}
