package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun BlogsListPage(
    podcasts: List<Podcast>,
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onAddBlogClick: () -> Unit,
    onPodcastClick: (uuid: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi02),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ThemedTopAppBar(
                title = stringResource(LR.string.profile_navigation_blogs),
                onNavigationClick = onBackPress,
            )

            val context = LocalContext.current
            val dateFormatter = remember(context) { RelativeDateFormatter(context) }

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = bottomInset + 16.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = podcasts, key = { it.uuid }) { podcast ->
                    BlogPodcastRow(
                        podcast = podcast,
                        dateFormatter = dateFormatter,
                        onClick = { onPodcastClick(podcast.uuid) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddBlogClick,
            backgroundColor = colors.primaryInteractive01,
            contentColor = colors.primaryInteractive02,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp + bottomInset),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_add_black_24dp),
                contentDescription = stringResource(LR.string.blogs_add_button),
            )
        }
    }
}

@Composable
private fun BlogPodcastRow(
    podcast: Podcast,
    dateFormatter: RelativeDateFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PodcastImage(
            uuid = podcast.uuid,
            imageSize = 56.dp,
            modifier = Modifier.size(56.dp),
        )
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            TextP40(
                text = podcast.title,
                maxLines = 1,
            )
            val date = podcast.latestEpisodeDate
            if (date != null) {
                TextP50(
                    text = dateFormatter.format(date),
                    maxLines = 1,
                    color = colors.primaryText02,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BlogsListPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        BlogsListPage(
            podcasts = listOf(
                Podcast(uuid = "1", title = "The Freeing Energy Podcast", latestEpisodeDate = Date()),
                Podcast(uuid = "2", title = "Talk Money To Me", latestEpisodeDate = Date(System.currentTimeMillis() - 86_400_000L)),
                Podcast(uuid = "3", title = "Zero To Travel Podcast", latestEpisodeDate = Date(System.currentTimeMillis() - 5L * 86_400_000L)),
            ),
            bottomInset = 0.dp,
            onBackPress = {},
            onAddBlogClick = {},
            onPodcastClick = {},
        )
    }
}
