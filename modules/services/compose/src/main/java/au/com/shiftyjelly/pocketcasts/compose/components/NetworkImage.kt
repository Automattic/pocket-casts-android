package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import coil3.compose.rememberAsyncImagePainter

@Composable
fun NetworkImage(
    imageUrl: String?,
    imageRequestSize: Dp,
    modifier: Modifier = Modifier,
    shadow: Shadow? = if (imageRequestSize < 100.dp) NetworkImageDefaults.DropShadowSmall else NetworkImageDefaults.DropShadow,
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl, imageRequestSize, context) {
        PocketCastsImageRequestFactory(
            context = context,
            placeholderType = PocketCastsImageRequestFactory.PlaceholderType.Small,
            size = imageRequestSize.value.toInt(),
        ).themed().createForFileOrUrl(imageUrl.orEmpty())
    }
    Image(
        painter = rememberAsyncImagePainter(imageRequest, contentScale = ContentScale.Crop),
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
            .aspectRatio(1f)
            .then(if (shadow != null) Modifier.dropShadow(CircleShape, shadow) else Modifier)
            .clip(CircleShape),
    )
}

object NetworkImageDefaults {
    val DropShadow = Shadow(
        radius = 8.dp,
        color = Color.Black,
        spread = 0.dp,
        offset = DpOffset(x = 0.dp, y = 2.dp),
        alpha = 0.15f,
    )
    val DropShadowSmall = Shadow(
        radius = 4.dp,
        color = Color.Black,
        spread = 0.dp,
        offset = DpOffset(x = 0.dp, y = 1.dp),
        alpha = 0.15f,
    )
}
