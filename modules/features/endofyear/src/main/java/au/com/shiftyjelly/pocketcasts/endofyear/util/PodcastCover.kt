package au.com.shiftyjelly.pocketcasts.endofyear.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage

@Composable
fun PodcastCoverSmall(
    uuid: String,
    coverWidth: Dp,
    modifier: Modifier = Modifier,
) {
    PodcastImage(
        uuid = uuid,
        cornerSize = 4.dp,
        modifier = modifier.size(coverWidth)
    )
}

@Composable
fun PodcastCoverBig(
    uuid: String,
    coverWidth: Dp,
    modifier: Modifier = Modifier,
) {
    PodcastImage(
        uuid = uuid,
        cornerSize = 8.dp,
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
