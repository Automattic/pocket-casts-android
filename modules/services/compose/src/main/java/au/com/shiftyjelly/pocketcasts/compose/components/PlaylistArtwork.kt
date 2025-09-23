package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlaylistArtwork(
    podcastUuids: List<String>,
    artworkSize: Dp,
    modifier: Modifier = Modifier,
    cornerSize: Dp = artworkSize / 14,
    elevation: Dp = 1.dp,
) {
    when (podcastUuids.size) {
        0 -> NoImage(
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            elevation = elevation,
            modifier = modifier,
        )

        in 1..3 -> SingleImage(
            podcastUuid = podcastUuids[0],
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            elevation = elevation,
            modifier = modifier,
        )

        else -> QuadImage(
            podcastUuid1 = podcastUuids[0],
            podcastUuid2 = podcastUuids[1],
            podcastUuid3 = podcastUuids[2],
            podcastUuid4 = podcastUuids[3],
            artworkSize = artworkSize,
            cornerSize = cornerSize,
            elevation = elevation,
            modifier = modifier,
        )
    }
}

@Composable
private fun NoImage(
    artworkSize: Dp,
    cornerSize: Dp,
    elevation: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(artworkSize)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, RoundedCornerShape(cornerSize))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(cornerSize))
            .background(MaterialTheme.theme.colors.primaryUi05)
            .semantics(mergeDescendants = true) {
                role = Role.Image
            },
    ) {
        Image(
            painter = painterResource(IR.drawable.ic_playlists),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            contentDescription = stringResource(LR.string.playlist_artwork_description),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03),
            modifier = Modifier.size(artworkSize * 0.43f),
        )
    }
}

@Composable
private fun SingleImage(
    podcastUuid: String,
    artworkSize: Dp,
    cornerSize: Dp,
    elevation: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(podcastUuid) {
        PocketCastsImageRequestFactory(context).themed().createForPodcast(podcastUuid)
    }
    Image(
        painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop),
        contentScale = ContentScale.Crop,
        alignment = Alignment.BottomCenter,
        contentDescription = stringResource(LR.string.playlist_artwork_description),
        modifier = modifier
            .size(artworkSize)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, RoundedCornerShape(cornerSize))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(cornerSize)),
    )
}

@Composable
private fun QuadImage(
    podcastUuid1: String,
    podcastUuid2: String,
    podcastUuid3: String,
    podcastUuid4: String,
    artworkSize: Dp,
    cornerSize: Dp,
    elevation: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val requestFactory = remember { PocketCastsImageRequestFactory(context).themed() }
    val imageRequest1 = remember(podcastUuid1) {
        requestFactory.createForPodcast(podcastUuid1)
    }
    val imageRequest2 = remember(podcastUuid2) {
        requestFactory.createForPodcast(podcastUuid2)
    }
    val imageRequest3 = remember(podcastUuid3) {
        requestFactory.createForPodcast(podcastUuid3)
    }
    val imageRequest4 = remember(podcastUuid4) {
        requestFactory.createForPodcast(podcastUuid4)
    }
    val artworkDescription = stringResource(LR.string.playlist_artwork_description)
    Row(
        modifier = modifier
            .size(artworkSize)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, RoundedCornerShape(cornerSize))
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                role = Role.Image
                contentDescription = artworkDescription
            },
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
                painter = rememberAsyncImagePainter(imageRequest3, contentScale = ContentScale.Crop),
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
                painter = rememberAsyncImagePainter(imageRequest2, contentScale = ContentScale.Crop),
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
            podcastUuids = emptyList(),
            artworkSize = 80.dp,
        )
    }
}

@Preview
@Composable
private fun PlaylistArtworkSingleEpisodePreview() {
    PlaylistArtwork(
        podcastUuids = listOf("podcast-uuid"),
        artworkSize = 80.dp,
    )
}

@Preview
@Composable
private fun PlaylistArtworkQuadEpisodePreview() {
    PlaylistArtwork(
        podcastUuids = List(4) { "podcat-uuid-$it" },
        artworkSize = 80.dp,
    )
}
