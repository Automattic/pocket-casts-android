package au.com.shiftyjelly.pocketcasts.discover.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast

@Composable
internal fun SmallListRow(
    pagerState: PagerState,
    podcasts: List<List<DiscoverPodcast>>,
    onClickPodcast: (DiscoverPodcast) -> Unit,
    onClickSubscribe: (DiscoverPodcast) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        modifier = modifier,
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
        ) { index ->
            Column {
                for (podcast in podcasts[index]) {
                    PodcastRow(
                        podcast = podcast,
                        onClickSubscribe = { onClickSubscribe(podcast) },
                        modifier = Modifier.clickable(onClick = { onClickPodcast(podcast) }),
                    )
                }
            }
        }

        PagerDotIndicator(
            state = pagerState,
            activeDotColor = MaterialTheme.theme.colors.primaryUi05Selected,
            dotWidth = pagerDotWidth,
        )
    }
}

@Composable
internal fun SmallListRowPlaceholder(
    podcastCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = itemSpacing + pagerDotWidth),
    ) {
        repeat(podcastCount) {
            PodcastRowPlaceholder()
        }
    }
}

private val itemSpacing = 4.dp
private val pagerDotWidth = 7.dp
