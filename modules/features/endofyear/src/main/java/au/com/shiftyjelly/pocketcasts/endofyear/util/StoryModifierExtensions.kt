package au.com.shiftyjelly.pocketcasts.endofyear.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer

private const val PodcastCoverRotationAngle = -30f
private const val PodcastCoverSkew = 0.45f

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
