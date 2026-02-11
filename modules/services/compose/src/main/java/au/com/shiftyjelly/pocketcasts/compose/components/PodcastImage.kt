package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import au.com.shiftyjelly.pocketcasts.localization.R as LR

fun podcastImageCornerSize(width: Dp): Dp {
    return when {
        width <= 50.dp -> 3.dp
        width <= 200.dp -> 4.dp
        else -> 8.dp
    }
}

/**
 * Displays a podcast artwork image with rounded corners and optional shadow.
 * When the image fails to load and a title is provided, shows a colored background with the title.
 *
 * @param uuid Podcast uuid.
 * @param modifier The modifier to be applied to the image.
 * @param title Optional podcast title to display when image loading fails.
 * @param imageSize The display size of the image in both width and height.
 * @param imageRequestSize The resolution to load from network/cache. Defaults to [imageSize].
 * Set to a fixed size when [imageSize] is animated to prevent reloading.
 * @param cornerSize The corner radius for rounding the image. Set to null for a rectangle shape.
 * @param elevation The shadow elevation applied to the image. Set to null to disable the shadow.
 * @param placeholderType The type of placeholder to show while the image is loading.
 * Automatically uses [PlaceholderType.Large] for images larger than 200.dp.
 * @param contentDescription The accessibility description for the image.
 */
@Composable
fun PodcastImage(
    uuid: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    imageSize: Dp = 56.dp,
    imageRequestSize: Dp = imageSize,
    cornerSize: Dp? = imageSize / 14,
    elevation: Dp? = 2.dp,
    placeholderType: PlaceholderType = if (imageSize > 200.dp) {
        PlaceholderType.Large
    } else {
        PlaceholderType.Small
    },
    contentDescription: String? = stringResource(LR.string.podcast_artwork_description),
) {
    val context = LocalContext.current
    val imageRequest = remember(uuid, placeholderType, imageRequestSize) {
        PocketCastsImageRequestFactory(
            context = context,
            placeholderType = placeholderType,
            size = imageRequestSize.value.toInt(),
        ).themed().createForPodcast(uuid)
    }
    val painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop)
    val shape = if (cornerSize != null) {
        RoundedCornerShape(cornerSize)
    } else {
        RectangleShape
    }
    
    Box(
        modifier = modifier
            .size(imageSize)
            .then(
                if (elevation != null) {
                    Modifier.shadow(elevation, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape),
    ) {
        Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
        
        // Show colored fallback when image fails to load and title is provided
        val state by painter.state.collectAsState()
        if (title != null && state is AsyncImagePainter.State.Error) {
            PodcastImageFallback(
                uuid = uuid,
                title = title,
                imageSize = imageSize,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Deprecated(message = "This component is fundamentally broken. Please use PodcastImage in new UI instead.")
@Composable
fun PodcastImageDeprecated(
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
    val contentDescriptionString = stringResource(LR.string.podcast_artwork_description)
    BoxWithConstraints(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Image
                contentDescription = contentDescriptionString
            },
    ) {
        val imageRequest = remember(uuid, maxWidth) {
            PocketCastsImageRequestFactory(
                context = context,
                size = maxWidth.value.toInt(),
                placeholderType = placeholderType,
            ).themed().createForPodcast(uuid)
        }

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
                    uuid = uuid,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            CoilImage(
                imageRequest = imageRequest,
                title = title,
                showTitle = showTitle,
                corners = corners,
                uuid = uuid,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Fallback UI for podcast image when loading fails.
 * Shows a colored background (using folder colors) with a solid overlay and the podcast title.
 */
@Composable
internal fun PodcastImageFallback(
    uuid: String,
    title: String,
    imageSize: Dp,
    modifier: Modifier = Modifier,
) {
    // Select color based on UUID hash, similar to how folders work
    val colors = MaterialTheme.theme.colors
    val colorIndex = remember(uuid) {
        kotlin.math.abs(uuid.hashCode()) % 12
    }
    val backgroundColor = colors.getFolderColor(colorIndex)
    
    // Determine font size based on image size
    val fontSize: TextUnit = remember(imageSize) {
        when {
            imageSize <= 56.dp -> 9.sp
            imageSize <= 120.dp -> 11.sp
            imageSize <= 200.dp -> 14.sp
            else -> 18.sp
        }
    }
    
    Box(
        modifier = modifier.background(backgroundColor),
    ) {
        // Solid overlay: Black at 20% opacity throughout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000)), // 20% opacity black
        )
        
        // Text in bottom left, white color
        Text(
            text = title,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.W600,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = imageSize * 0.08f,
                    end = imageSize * 0.08f,
                    bottom = imageSize * 0.08f,
                ),
        )
    }
}
