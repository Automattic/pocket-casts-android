package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage

private const val PodcastCoverRotationAngle = -30f
private const val PodcastCoverSkew = 0.45f

@Composable
fun PodcastCover(
    uuid: String,
    coverWidth: Dp,
    coverType: PodcastCoverType,
    modifier: Modifier = Modifier,
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
    backgroundColor: Color,
    modifier: Modifier = Modifier,
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
    graphicsLayer(rotationZ = PodcastCoverRotationAngle)
        .drawWithContent {
            withTransform({
                val transformMatrix = Matrix()
                transformMatrix.values[Matrix.SkewX] = PodcastCoverSkew
                transform(transformMatrix)
            }
            ) {
                this@drawWithContent.drawContent()
            }
        }

enum class PodcastCoverType {
    SMALL,
    BIG
}
