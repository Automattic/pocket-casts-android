package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import coil.compose.rememberAsyncImagePainter

@Composable
fun PodcastImage(
    uuid: String,
    modifier: Modifier = Modifier,
    roundCorners: Boolean = true,
    dropShadow: Boolean = true
) {
    BoxWithConstraints(modifier) {
        val corners = when {
            !roundCorners -> null
            maxWidth <= 50.dp -> 3.dp
            maxWidth <= 200.dp -> 4.dp
            else -> 8.dp
        }
        if (dropShadow) {
            val elevation = when {
                maxWidth <= 50.dp -> 1.dp
                maxWidth <= 200.dp -> 2.dp
                else -> 4.dp
            }
            Card(
                elevation = elevation,
                shape = if (corners == null) RectangleShape else RoundedCornerShape(corners),
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            ) {
                CoilImage(
                    uuid = uuid,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            CoilImage(
                uuid = uuid,
                corners = corners,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CoilImage(uuid: String, modifier: Modifier = Modifier, corners: Dp? = null) {
    val context = LocalContext.current
    Image(
        painter = rememberAsyncImagePainter(
            PodcastImageLoaderThemed(context).loadCoil(podcastUuid = uuid).build(),
            contentScale = ContentScale.Crop
        ),
        contentDescription = null,
        modifier = modifier
            .clip(if (corners == null) RectangleShape else RoundedCornerShape(corners))
    )
}
