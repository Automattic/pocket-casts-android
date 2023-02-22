package au.com.shiftyjelly.pocketcasts.compose.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage

private val gradientTop = Color(0x00000000)
private val gradientBottom = Color(0xFF000000)
private val topPodcastImageGradient = listOf(Color(0x00000000), Color(0x16000000))
private val bottomPodcastImageGradient = listOf(Color(0x16000000), Color(0x33000000))

private val FolderImageSize = 64.dp
private val PodcastImageSize = 26.dp
@Composable
fun FolderImageSmall(
    color: Color,
    podcastUuids: List<String>,
    modifier: Modifier = Modifier,
    folderImageSize: Dp = FolderImageSize,
    podcastImageSize: Dp = PodcastImageSize,
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(4.dp),
        backgroundColor = color,
        modifier = modifier.size(folderImageSize)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .alpha(0.2f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                gradientTop,
                                gradientBottom
                            )
                        )
                    )
                    .size(folderImageSize)
            ) {}
            Row(horizontalArrangement = Arrangement.Center) {
                val imagePadding = 2.dp
                Column(horizontalAlignment = Alignment.End) {
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(0),
                        color = color,
                        gradientColor = topPodcastImageGradient,
                        modifier = Modifier.size(podcastImageSize)
                    )
                    Spacer(modifier = Modifier.height(imagePadding))
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(2),
                        color = color,
                        gradientColor = bottomPodcastImageGradient,
                        modifier = Modifier.size(podcastImageSize)
                    )
                }
                Spacer(modifier = Modifier.width(imagePadding))
                Column(horizontalAlignment = Alignment.Start) {
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(1),
                        color = color,
                        gradientColor = topPodcastImageGradient,
                        modifier = Modifier.size(podcastImageSize)
                    )
                    Spacer(modifier = Modifier.height(imagePadding))
                    FolderPodcastImage(
                        uuid = podcastUuids.getOrNull(3),
                        color = color,
                        gradientColor = bottomPodcastImageGradient,
                        modifier = Modifier.size(podcastImageSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderPodcastImage(
    uuid: String?,
    color: Color,
    gradientColor: List<Color>,
    modifier: Modifier = Modifier
) {
    if (uuid == null) {
        BoxWithConstraints(modifier) {
            Card(
                elevation = 1.dp,
                shape = RoundedCornerShape(3.dp),
                backgroundColor = color,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(maxWidth)
                        .background(brush = Brush.verticalGradient(colors = gradientColor))
                ) {}
                Box(
                    modifier = Modifier
                        .size(maxWidth)
                        .background(color = Color(0x19000000))
                ) {}
            }
        }
    } else {
        PodcastImage(
            uuid = uuid,
            modifier = modifier
        )
    }
}
