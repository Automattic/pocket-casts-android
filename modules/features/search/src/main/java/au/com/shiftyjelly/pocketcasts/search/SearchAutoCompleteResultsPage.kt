package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.converter.SafeDate
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun SearchAutoCompleteResultsPage(
    searchTerm: String,
    isLoading: Boolean,
    results: List<SearchAutoCompleteItem>,
    onTermClick: (SearchAutoCompleteItem.Term) -> Unit,
    onPodcastClick: (SearchAutoCompleteItem.Podcast) -> Unit,
    onPodcastFollow: (SearchAutoCompleteItem.Podcast) -> Unit,
    onEpisodeClick: (SearchAutoCompleteItem.Episode) -> Unit,
    onEpisodePlay: (SearchAutoCompleteItem.Episode) -> Unit,
    bottomInset: Dp,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                onScroll()
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = isLoading,
            modifier = Modifier
                .padding(vertical = 32.dp)
                .align(Alignment.Center),
        ) {
            CircularProgressIndicator()
        }
        LazyColumn(
            modifier = Modifier.nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            results.forEachIndexed { index, item ->
                item(contentType = "content-${item.javaClass}") {
                    when (item) {
                        is SearchAutoCompleteItem.Term -> SearchTermRow(
                            searchTerm = searchTerm,
                            item = item,
                            onClick = { onTermClick(item) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                        )

                        is SearchAutoCompleteItem.Podcast -> PodcastRow(
                            item = item,
                            onClick = { onPodcastClick(item) },
                            onFollow = { onPodcastFollow(item) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        is SearchAutoCompleteItem.Episode -> EpisodeRow(
                            item = item,
                            onClick = { onEpisodeClick(item) },
                            onPlay = { onEpisodePlay(item) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (results.indices.last != index) {
                    item(contentType = "divider") {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.theme.colors.secondaryText02,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTermRow(
    searchTerm: String,
    item: SearchAutoCompleteItem.Term,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.theme.colors.primaryText01
    val label = remember(searchTerm, item) {
        buildAnnotatedString {
            append(item.term)
            Regex(Regex.escape(searchTerm), RegexOption.IGNORE_CASE).findAll(item.term).forEach { result ->
                if (result.range.start < result.range.endInclusive + 1) {
                    addStyle(
                        style = SpanStyle(color = primaryColor),
                        start = result.range.first,
                        end = result.range.endInclusive + 1,
                    )
                }
            }
        }
    }

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(IR.drawable.ic_search), contentDescription = null, tint = MaterialTheme.theme.colors.primaryText01)
        TextH40(
            text = label,
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

@Composable
private fun PodcastRow(
    item: SearchAutoCompleteItem.Podcast,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PodcastImage(
            uuid = item.uuid,
            elevation = 6.dp,
            cornerSize = 4.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            TextH40(
                text = item.title,
                color = MaterialTheme.theme.colors.primaryText01,
            )
            TextP50(
                text = item.author,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
        if (item.isSubscribed) {
            Icon(
                modifier = Modifier
                    .minimumInteractiveComponentSize(),
                painter = painterResource(IR.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.support02,
            )
        } else {
            IconButton(onClick = onFollow) {
                Icon(
                    painter = painterResource(IR.drawable.ic_add_black_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.secondaryText02,
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    item: SearchAutoCompleteItem.Episode,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EpisodeImage(
            episode = PodcastEpisode(
                uuid = "",
                publishedDate = SafeDate(0),
            ),
            useEpisodeArtwork = false,
            corners = 4.dp,
            modifier = Modifier.shadow(elevation = 6.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextC70(
                text = "EPISODE DATE",
            )
            TextH40(
                text = "Episode title",
                color = MaterialTheme.theme.colors.primaryText01,
            )
            TextH60(
                text = "Duration",
                color = MaterialTheme.theme.colors.secondaryText02,
                fontWeight = FontWeight.W600,
            )
        }
        Icon(
            painter = painterResource(IR.drawable.filter_play),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(1.dp, color = MaterialTheme.theme.colors.primaryInteractive01, shape = CircleShape)
                .clickable(onClick = onPlay),
            tint = MaterialTheme.theme.colors.primaryInteractive01,
        )
    }
}

@Preview
@Composable
private fun PreviewSearchAutoCompleteResultsPage(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchAutoCompleteResultsPage(
            isLoading = false,
            searchTerm = "matching",
            results = listOf(
                SearchAutoCompleteItem.Term("matching text"),
                SearchAutoCompleteItem.Term("matching text longer"),
                SearchAutoCompleteItem.Term("text only matching later"),
                SearchAutoCompleteItem.Term("this doesn't match but why is it returned then?"),
                SearchAutoCompleteItem.Podcast(uuid = "", title = "Matching podcast", author = "Author", isSubscribed = false),
                SearchAutoCompleteItem.Podcast(uuid = "", title = "Matching podcast subscribed", author = "Author2", isSubscribed = true),
            ),
            onTermClick = {},
            onEpisodePlay = {},
            onEpisodeClick = {},
            onPodcastClick = {},
            onPodcastFollow = {},
            onScroll = {},
            bottomInset = 0.dp,
        )
    }
}
