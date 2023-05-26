package au.com.shiftyjelly.pocketcasts.wear.ui.podcasts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.extensions.darker
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object PodcastsScreen {
    const val route = "podcasts_screen"
}

@Composable
fun PodcastsScreen(
    modifier: Modifier = Modifier,
    viewModel: PodcastsViewModel = hiltViewModel(),
    listState: ScalingLazyListState,
    navigateToPodcast: (String) -> Unit
) {
    val uiState = viewModel.uiState

    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState
    ) {
        item {
            Text(
                text = stringResource(LR.string.podcasts),
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.theme.colors.primaryText01,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(items = uiState.items, key = { item -> item.uuid }) { item ->
            if (item is FolderItem.Podcast) {
                PodcastChip(podcast = item, onClick = navigateToPodcast)
            }
        }
    }
}

@Composable
private fun PodcastChip(
    podcast: FolderItem.Podcast,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Chip(
        onClick = { onClick(podcast.uuid) },
        colors = ChipDefaults.gradientBackgroundChipColors(
            startBackgroundColor = Color(podcast.podcast.tintColorForDarkBg).darker(0.5f),
            endBackgroundColor = MaterialTheme.colors.surface
        ),
        label = {
            Text(
                text = podcast.title,
                maxLines = 2,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        },
        icon = {
            PodcastImage(
                uuid = podcast.uuid,
                dropShadow = false,
                modifier = Modifier.size(32.dp)
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}
