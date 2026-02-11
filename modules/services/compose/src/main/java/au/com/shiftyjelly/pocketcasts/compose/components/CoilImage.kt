package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest

@Composable
internal fun CoilImage(
    imageRequest: ImageRequest,
    title: String,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    corners: Dp? = null,
    expandContentSize: Boolean = false,
    uuid: String = "", // UUID for fallback color selection
) {
    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        contentScale = contentScale,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Image(
            painter = painter,
            contentDescription = title,
            modifier = Modifier
                .then(if (expandContentSize) Modifier.fillMaxSize() else Modifier)
                .clip(if (corners == null) RectangleShape else RoundedCornerShape(corners)),
        )
        val state by painter.state.collectAsState()
        if (showTitle && state is AsyncImagePainter.State.Error) {
            PodcastImageFallback(
                uuid = uuid,
                title = title,
                imageSize = 120.dp, // Default size, will be constrained by parent
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
