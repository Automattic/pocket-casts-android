package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddEpisodesViewModel.PodcastEpisodesUiState
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun AddEpisodesColumn(
    uiState: PodcastEpisodesUiState?,
    useEpisodeArtwork: Boolean,
    onAddEpisode: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val topItem = remember(uiState?.episodes) { uiState?.episodes?.firstOrNull()?.uuid }
    LaunchedEffect(topItem) {
        if (topItem != null && listState.firstVisibleItemIndex != 0) {
            listState.scrollToItem(0)
        }
    }

    FadedLazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        if (!uiState?.episodes.isNullOrEmpty()) {
            items(
                items = uiState.episodes,
                key = { episode -> episode.uuid },
                contentType = { _ -> "episode" },
            ) { episode ->
                EpisodeRow(
                    episode = episode,
                    onClickAdd = { onAddEpisode(episode) },
                    useEpisodeArtwork = useEpisodeArtwork,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (uiState?.episodes?.isEmpty() == true) {
            item(key = "no-content", contentType = "no-content") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                ) {
                    NoContentBanner(
                        title = if (uiState.unfilteredEpisodeCount == 0) {
                            stringResource(LR.string.manual_playlist_no_available_episode_title)
                        } else {
                            stringResource(LR.string.manual_playlist_search_no_episode_title)
                        },
                        body = if (uiState.unfilteredEpisodeCount == 0) {
                            stringResource(LR.string.manual_playlist_no_available_episode_body)
                        } else {
                            stringResource(LR.string.manual_playlist_search_no_episode_body)
                        },
                        iconResourceId = IR.drawable.ic_exclamation_circle,
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: PodcastEpisode,
    useEpisodeArtwork: Boolean,
    onClickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            EpisodeImage(
                episode = episode,
                useEpisodeArtwork = useEpisodeArtwork,
                placeholderType = PlaceholderType.Small,
                corners = 4.dp,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp)),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                TextC70(
                    text = episode.rememberHeaderText(),
                )
                TextH40(
                    text = episode.title,
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
                TextH60(
                    text = episode.rememberTimeLeftText(),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            IconButton(
                onClick = onClickAdd,
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_add_episode),
                    contentDescription = stringResource(LR.string.add_to_playlist_episode_desc, episode.title),
                    tint = MaterialTheme.theme.colors.primaryText01,
                )
            }
        }
        HorizontalDivider(
            startIndent = 12.dp,
        )
    }
}

@Composable
private fun PodcastEpisode.rememberHeaderText(): AnnotatedString {
    val context = LocalContext.current
    val formatter = remember(context) { RelativeDateFormatter(context) }
    return remember(playingStatus) {
        val tintColor = context.getThemeColor(UR.attr.primary_icon_01)
        val spannable = getSummaryText(formatter, tintColor, showDuration = false, context)
        spannable.toAnnotatedString()
    }
}

@Composable
private fun PodcastEpisode.rememberTimeLeftText(): String {
    val context = LocalContext.current
    return remember(playedUpToMs, durationMs, isInProgress, context) {
        TimeHelper.getTimeLeft(playedUpToMs, durationMs.toLong(), isInProgress, context).text
    }
}

@Preview
@Composable
private fun EpisodesColumnNoAvailablePreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        AddEpisodesColumn(
            uiState = PodcastEpisodesUiState(
                unfilteredEpisodeCount = 0,
                episodes = emptyList(),
            ),
            useEpisodeArtwork = false,
            onAddEpisode = {},
        )
    }
}

@Preview
@Composable
private fun EpisodesColumnNoFoundPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        AddEpisodesColumn(
            uiState = PodcastEpisodesUiState(
                unfilteredEpisodeCount = 3,
                episodes = emptyList(),
            ),
            useEpisodeArtwork = false,
            onAddEpisode = {},
        )
    }
}

@Preview
@Composable
private fun EpisodesColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddEpisodesColumn(
            uiState = PodcastEpisodesUiState(
                unfilteredEpisodeCount = 3,
                episodes = List(3) { index ->
                    PodcastEpisode(
                        uuid = "uuid-$index",
                        title = "Episode $index",
                        duration = 1200.0,
                        publishedDate = Date(0),
                    )
                },
            ),
            useEpisodeArtwork = false,
            onAddEpisode = {},
        )
    }
}
