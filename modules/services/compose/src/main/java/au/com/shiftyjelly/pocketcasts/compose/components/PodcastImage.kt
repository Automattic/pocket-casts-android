package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed

fun podcastImageCornerSize(width: Dp): Dp {
    return when {
        width <= 50.dp -> 3.dp
        width <= 200.dp -> 4.dp
        else -> 8.dp
    }
}

@Composable
fun PodcastImage(
    uuid: String,
    modifier: Modifier = Modifier,
    title: String = "", // also used as the image's content description
    showTitle: Boolean = false,
    roundCorners: Boolean = true,
    dropShadow: Boolean = true,
    cornerSize: Dp? = null,
    elevation: Dp? = null,
    placeholderType: PlaceholderType = PlaceholderType.Large,
) {
    val context = LocalContext.current

    val imageRequest = remember(uuid) {
        PocketCastsImageRequestFactory(context, placeholderType = placeholderType).themed().createForPodcast(uuid)
    }

    BoxWithConstraints(modifier = modifier) {
        val corners = if (roundCorners) cornerSize ?: podcastImageCornerSize(maxWidth) else null
        if (dropShadow) {
            val finalElevation = elevation ?: when {
                maxWidth <= 50.dp -> 1.dp
                maxWidth <= 200.dp -> 2.dp
                else -> 4.dp
            }
            Card(
                elevation = finalElevation,
                shape = if (corners == null) RectangleShape else RoundedCornerShape(corners),
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
            ) {
                CoilImage(
                    imageRequest = imageRequest,
                    title = title,
                    showTitle = showTitle,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            CoilImage(
                imageRequest = imageRequest,
                title = title,
                showTitle = showTitle,
                corners = corners,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
