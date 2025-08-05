package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil.compose.rememberAsyncImagePainter
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun PlaylistArtwork(
    podcasts: List<Podcast>,
    artworkSize: Dp,
    modifier: Modifier = Modifier,
    cornerSize: Dp = artworkSize / 14,
) {
    when (podcasts.size) {
        0 -> NoImage(
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            modifier = modifier,
        )

        in 1..3 -> SingleImage(
            podcast = podcasts[0],
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            modifier = modifier,
        )

        else -> QuadImage(
            podcast1 = podcasts[0],
            podcast2 = podcasts[1],
            podcast3 = podcasts[2],
            podcast4 = podcasts[3],
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
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
            modifier = Modifier.size(artworkSize * 0.43f),
        )
    }
}

@Composable
private fun SingleImage(
    podcast: Podcast,
    artworkSize: Dp,
    cornerSize: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(podcast.uuid) {
        PocketCastsImageRequestFactory(context).themed().create(podcast)
    }
    Image(
        painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop),
        contentScale = ContentScale.Crop,
        alignment = Alignment.BottomCenter,
        contentDescription = null,
        modifier = modifier
            .size(artworkSize)
            .clip(RoundedCornerShape(cornerSize)),
    )
}

@Composable
private fun QuadImage(
    podcast1: Podcast,
    podcast2: Podcast,
    podcast3: Podcast,
    podcast4: Podcast,
    artworkSize: Dp,
    cornerSize: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val requestFactory = remember { PocketCastsImageRequestFactory(context).themed() }
    val imageRequest1 = remember(podcast1.uuid) {
        requestFactory.create(podcast1)
    }
    val imageRequest2 = remember(podcast2.uuid) {
        requestFactory.create(podcast2)
    }
    val imageRequest3 = remember(podcast3.uuid) {
        requestFactory.create(podcast3)
    }
    val imageRequest4 = remember(podcast4.uuid) {
        requestFactory.create(podcast4)
    }
    Row(
        modifier = modifier.size(artworkSize),
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(imageRequest1, contentScale = ContentScale.Crop),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize / 2)
                    .clip(RoundedCornerShape(topStart = cornerSize)),
            )
            Image(
                painter = rememberAsyncImagePainter(imageRequest2, contentScale = ContentScale.Crop),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize / 2)
                    .clip(RoundedCornerShape(bottomStart = cornerSize)),
            )
        }
        Column {
            Image(
                painter = rememberAsyncImagePainter(imageRequest3, contentScale = ContentScale.Crop),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize / 2)
                    .clip(RoundedCornerShape(topEnd = cornerSize)),
            )
            Image(
                painter = rememberAsyncImagePainter(imageRequest4, contentScale = ContentScale.Crop),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize / 2)
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
            podcasts = emptyList(),
            artworkSize = 80.dp,
        )
    }
}

@Preview
@Composable
private fun PlaylistArtworkSingleEpisodePreview() {
    PlaylistArtwork(
        podcasts = List(1) { Podcast(uuid = "$it") },
        artworkSize = 80.dp,
    )
}

@Preview
@Composable
private fun PlaylistArtworkQuadEpisodePreview() {
    PlaylistArtwork(
        podcasts = List(4) { Podcast(uuid = "$it", Date()) },
        artworkSize = 80.dp,
    )
}
