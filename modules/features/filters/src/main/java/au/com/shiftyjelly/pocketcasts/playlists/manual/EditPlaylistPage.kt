package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun EditPlaylistPage(
    episodes: List<PlaylistEpisode>,
    useEpisodeArtwork: Boolean,
    onUpdateEpisodes: (List<PlaylistEpisode>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }

    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .fillMaxSize(),
    ) {
        ThemedTopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = "Edit playlist",
            navigationButton = NavigationButton.Back,
            style = ThemedTopAppBar.Style.Immersive,
            backgroundColor = Color.Transparent,
            windowInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Vertical),
        )
        FadedLazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = episodes,
                key = { _, episode -> episode.uuid },
            ) { index, episode ->
                EpisodeRow(
                    episodeWrapper = episode,
                    useEpisodeArtwork = useEpisodeArtwork,
                    showDivider = index != episodes.lastIndex,
                    dateFormatter = dateFormatter,
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episodeWrapper: PlaylistEpisode,
    useEpisodeArtwork: Boolean,
    showDivider: Boolean,
    dateFormatter: RelativeDateFormatter,
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
                .padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 16.dp),
        ) {
            IconButton(
                onClick = {},
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_playlist_remove_episode),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.support05,
                )
            }
            PlaylistEpisodeImage(
                episodeWrapper = episodeWrapper,
                useEpisodeArtwork = useEpisodeArtwork,
                modifier = Modifier.alphaIfUnavailable(episodeWrapper),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alphaIfUnavailable(episodeWrapper),
            ) {
                TextC70(
                    text = episodeWrapper.rememberHeaderText(dateFormatter),
                )
                TextH40(
                    text = episodeWrapper.title(),
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
                TextH60(
                    text = episodeWrapper.rememberFooterText(),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Image(
                painter = painterResource(IR.drawable.ic_playlist_edit),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
                modifier = Modifier.size(24.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(startIndent = 16.dp)
        }
    }
}

@Composable
private fun PlaylistEpisodeImage(
    episodeWrapper: PlaylistEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    when (episodeWrapper) {
        is PlaylistEpisode.Available -> {
            EpisodeImage(
                episode = episodeWrapper.episode,
                useEpisodeArtwork = useEpisodeArtwork,
                placeholderType = PlaceholderType.Small,
                corners = 4.dp,
                modifier = modifier
                    .size(56.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp)),
            )
        }

        is PlaylistEpisode.Unavailable -> {
            PodcastImage(
                uuid = episodeWrapper.episode.podcastUuid,
                placeholderType = PlaceholderType.Small,
                cornerSize = 4.dp,
                modifier = modifier
                    .size(56.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun PlaylistEpisode.rememberHeaderText(formatter: RelativeDateFormatter): AnnotatedString {
    return when (this) {
        is PlaylistEpisode.Available -> remember(episode.playingStatus, formatter) {
            val tintColor = formatter.context.getThemeColor(UR.attr.primary_icon_01)
            val spannable = episode.getSummaryText(formatter, tintColor, showDuration = false, formatter.context)
            spannable.toAnnotatedString()
        }

        is PlaylistEpisode.Unavailable -> remember(episode.publishedAt, formatter) {
            AnnotatedString(formatter.format(Date.from(episode.publishedAt)))
        }
    }
}

private fun PlaylistEpisode.title(): String {
    return when (this) {
        is PlaylistEpisode.Available -> episode.title
        is PlaylistEpisode.Unavailable -> episode.title
    }
}

@Composable
private fun PlaylistEpisode.rememberFooterText(): String {
    val context = LocalContext.current
    return when (this) {
        is PlaylistEpisode.Available -> remember(episode.playedUpToMs, episode.durationMs, episode.isInProgress, context) {
            TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context).text
        }

        is PlaylistEpisode.Unavailable -> stringResource(LR.string.unavailable)
    }
}

private fun Modifier.alphaIfUnavailable(episodeWrapper: PlaylistEpisode) = when (episodeWrapper) {
    is PlaylistEpisode.Available -> this
    is PlaylistEpisode.Unavailable -> alpha(0.4f)
}

@Preview
@Composable
private fun EditPlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var episodes by remember {
        mutableStateOf(
            List(8) { index ->
                if (index % 3 != 0) {
                    PlaylistEpisode.Available(
                        PodcastEpisode(
                            uuid = "episode-id-$index",
                            title = "Episode $index",
                            publishedDate = Date(0),
                            duration = 1234.0 * index,
                        ),
                    )
                } else {
                    PlaylistEpisode.Unavailable(
                        ManualPlaylistEpisode.test(
                            episodeUuid = "episode-id-$index",
                            title = "Episode $index",
                        ),
                    )
                }
            },
        )
    }

    AppTheme(themeType) {
        EditPlaylistPage(
            episodes = episodes,
            useEpisodeArtwork = false,
            onUpdateEpisodes = { episodes = it },
        )
    }
}
