package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PodcastCover(
    uuid: String,
    coverWidth: Dp,
    modifier: Modifier = Modifier,
    coverType: PodcastCoverType = PodcastCoverType.SMALL,
) {
    PodcastImage(
        uuid = uuid,
        cornerSize = if (coverType == PodcastCoverType.SMALL) 4.dp else 8.dp,
        modifier = modifier.size(coverWidth)
    )
}

@Composable
fun RectangleCover(
    coverWidth: Dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
) {
    Box(
        modifier = modifier.size(coverWidth)
    ) {
        val elevation = when {
            coverWidth <= 50.dp -> 1.dp
            coverWidth <= 200.dp -> 2.dp
            else -> 4.dp
        }
        Card(
            elevation = elevation,
            shape = RoundedCornerShape(8.dp),
            backgroundColor = backgroundColor,
            modifier = modifier.fillMaxSize()
        ) {}
    }
}

fun Modifier.transformPodcastCover() =
    drawWithContent {
        withTransform({
            scale(1f, .6f)
            rotate(-45f)
            scale(1.25f, 1.25f)
        }) {
            this@drawWithContent.drawContent()
        }
    }

enum class PodcastCoverType {
    SMALL,
    BIG
}
