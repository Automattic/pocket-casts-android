package au.com.shiftyjelly.pocketcasts.compose.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
fun FolderImageSmall(
    color: Color,
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    elevation: Dp? = 2.dp,
) {
    val cornerSize = size / 14
    val shape = RoundedCornerShape(cornerSize)
    val estimatedPadding = size / 12f
    val artworkSize = (size - estimatedPadding * 3) / 2
    FlowRow(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
        maxItemsInEachRow = 2,
        modifier = modifier
            .size(size)
            .then(
                if (elevation != null) {
                    Modifier.shadow(elevation, shape)
                } else {
                    Modifier
                },
            )
            .background(color, shape)
            .background(BackgroundGradient, shape),
    ) {
        PodcastOrPlaceHolder(
            podcastUuid = podcastUuids.getOrNull(0),
            size = artworkSize,
            cornerSize = cornerSize,
            placeholderBrush = TopPlaceholderGradient,
        )
        PodcastOrPlaceHolder(
            podcastUuid = podcastUuids.getOrNull(1),
            size = artworkSize,
            cornerSize = cornerSize,
            placeholderBrush = TopPlaceholderGradient,
        )
        PodcastOrPlaceHolder(
            podcastUuid = podcastUuids.getOrNull(2),
            size = artworkSize,
            cornerSize = cornerSize,
            placeholderBrush = BottomPlaceholderGradient,
        )
        PodcastOrPlaceHolder(
            podcastUuid = podcastUuids.getOrNull(3),
            size = artworkSize,
            cornerSize = cornerSize,
            placeholderBrush = BottomPlaceholderGradient,
        )
    }
}

@Composable
private fun PodcastOrPlaceHolder(
    podcastUuid: String?,
    size: Dp,
    cornerSize: Dp,
    placeholderBrush: Brush,
) {
    if (podcastUuid != null) {
        PodcastImage(
            uuid = podcastUuid,
            imageSize = size,
            cornerSize = cornerSize,
            elevation = null,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(placeholderBrush, RoundedCornerShape(cornerSize)),
        )
    }
}

private val BackgroundGradient = Brush.verticalGradient(colors = listOf(Color(0x00000000), Color(0x33000000)))
private val TopPlaceholderGradient = Brush.verticalGradient(colors = listOf(Color(0x08000000), Color(0x16000000)))
private val BottomPlaceholderGradient = Brush.verticalGradient(colors = listOf(Color(0x16000000), Color(0x32000000)))

@Preview
@Composable
private fun FolderImageSmallPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .background(Color.White)
            .padding(16.dp),
    ) {
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(0),
            podcastUuids = emptyList(),
        )
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(1),
            podcastUuids = List(1) { "$it" },
        )
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(2),
            podcastUuids = List(2) { "$it" },
        )
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(3),
            podcastUuids = List(3) { "$it" },
        )
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(4),
            podcastUuids = List(4) { "$it" },
        )
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(5),
            podcastUuids = List(4) { "$it" },
            size = 120.dp,
        )
    }
}
