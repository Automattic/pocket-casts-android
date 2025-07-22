package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil.compose.rememberAsyncImagePainter
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun PlaylistArtwork(
    episodes: List<BaseEpisode>,
    artworkSize: Dp,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    val cornerSize = artworkSize / 14
    when (episodes.size) {
        0 -> NoImage(
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            modifier = modifier,
        )

        in 1..3 -> SingleImage(
            episode = episodes[0],
            useEpisodeArtwork = useEpisodeArtwork,
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            modifier = modifier,
        )

        else -> QuadImage(
            episode1 = episodes[0],
            episode2 = episodes[1],
            episode3 = episodes[2],
            episode4 = episodes[3],
            useEpisodeArtwork = useEpisodeArtwork,
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            modifier = modifier,
        )
    }
}

@Composable
private fun NoImage(
    artworkSize: Dp,
    cornerSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(artworkSize)
            .clip(RoundedCornerShape(cornerSize))
            .background(MaterialTheme.theme.colors.primaryUi05),
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_playlists),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
            modifier = Modifier.size(artworkSize * 0.43f),
        )
    }
}

@Composable
private fun SingleImage(
    episode: BaseEpisode,
    artworkSize: Dp,
    cornerSize: Dp,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(episode.uuid, useEpisodeArtwork) {
        PocketCastsImageRequestFactory(context).themed().create(episode, useEpisodeArtwork)
    }
    Image(
        painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop),
        contentDescription = null,
        modifier = modifier
            .size(artworkSize)
            .clip(RoundedCornerShape(cornerSize)),
    )
}

@Composable
private fun QuadImage(
    episode1: BaseEpisode,
    episode2: BaseEpisode,
    episode3: BaseEpisode,
    episode4: BaseEpisode,
    artworkSize: Dp,
    cornerSize: Dp,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val requestFactory = remember { PocketCastsImageRequestFactory(context).themed() }
    val imageRequest1 = remember(episode1.uuid, useEpisodeArtwork) {
        requestFactory.create(episode1, useEpisodeArtwork)
    }
    val imageRequest2 = remember(episode2.uuid, useEpisodeArtwork) {
        requestFactory.create(episode2, useEpisodeArtwork)
    }
    val imageRequest3 = remember(episode3.uuid, useEpisodeArtwork) {
        requestFactory.create(episode3, useEpisodeArtwork)
    }
    val imageRequest4 = remember(episode4.uuid, useEpisodeArtwork) {
        requestFactory.create(episode4, useEpisodeArtwork)
    }
    Row(
        modifier = modifier.size(artworkSize),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageRequest1, contentScale = ContentScale.Crop),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(topStart = cornerSize)),
            )
            Image(
                painter = rememberAsyncImagePainter(imageRequest2, contentScale = ContentScale.Crop),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(bottomStart = cornerSize)),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageRequest3, contentScale = ContentScale.Crop),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(topEnd = cornerSize)),
            )
            Image(
                painter = rememberAsyncImagePainter(imageRequest4, contentScale = ContentScale.Crop),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(bottomEnd = cornerSize)),
            )
        }
    }
}

@Preview
@Composable
private fun PlaylistArtworkNoEpisodePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        PlaylistArtwork(
            episodes = emptyList(),
            artworkSize = 80.dp,
            useEpisodeArtwork = false,
        )
    }
}

@Preview
@Composable
private fun PlaylistArtworkSingleEpisodePreview() {
    PlaylistArtwork(
        episodes = List(1) { PodcastEpisode(uuid = "$it", publishedDate = Date()) },
        artworkSize = 80.dp,
        useEpisodeArtwork = false,
    )
}

@Preview
@Composable
private fun PlaylistArtworkQuadEpisodePreview() {
    PlaylistArtwork(
        episodes = List(4) { PodcastEpisode(uuid = "$it", publishedDate = Date()) },
        artworkSize = 80.dp,
        useEpisodeArtwork = false,
    )
}
