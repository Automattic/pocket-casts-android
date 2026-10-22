package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun TvArtworkImage(
    model: Any?,
    modifier: Modifier = Modifier,
) {
    val placeholder = remember { ColorPainter(Color.Black.copy(alpha = 0.2f)) }
    AsyncImage(
        model = model,
        placeholder = placeholder,
        error = placeholder,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
