package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
internal fun CoilImage(
    imageRequest: ImageRequest,
    title: String,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    corners: Dp? = null,
    expandContentSize: Boolean = false,
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
        val state = painter.state
        if (showTitle && state is AsyncImagePainter.State.Error) {
            TextP60(
                text = title,
                textAlign = TextAlign.Center,
                maxLines = 4,
                modifier = Modifier.padding(2.dp),
            )
        }
    }
}
