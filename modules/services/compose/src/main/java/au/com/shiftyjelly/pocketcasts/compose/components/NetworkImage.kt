package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.rememberAsyncImagePainter

/**
 * Displays a podcast network image cropped to a circle with an optional drop shadow.
 *
 * @param imageUrl The URL of the network image. A placeholder is shown when null or loading.
 * @param imageRequestSize The resolution to load from network/cache, usually the display size of the image.
 * @param modifier The modifier to be applied to the image. The image is always square.
 * @param shadow The drop shadow drawn behind the image. Set to null to disable the shadow.
 * Defaults to [NetworkImageDefaults.DropShadowSmall] for images smaller than 100.dp, otherwise
 * [NetworkImageDefaults.DropShadow].
 */
@Composable
fun NetworkImage(
    imageUrl: String?,
    imageRequestSize: Dp,
    modifier: Modifier = Modifier,
    shadow: Shadow? = if (imageRequestSize < 100.dp) {
        NetworkImageDefaults.DropShadowSmall
    } else {
        NetworkImageDefaults.DropShadow
    },
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
    val DropShadow: Shadow
        @Composable get() = Shadow(
            radius = 8.dp,
            color = Color.Black,
            spread = 0.dp,
            offset = DpOffset(x = 0.dp, y = 2.dp),
            alpha = shadowAlpha,
        )

    val DropShadowSmall: Shadow
        @Composable get() = Shadow(
            radius = 4.dp,
            color = Color.Black,
            spread = 0.dp,
            offset = DpOffset(x = 0.dp, y = 1.dp),
            alpha = shadowAlpha,
        )

    private val shadowAlpha: Float
        @Composable get() = if (MaterialTheme.theme.isDark) 0.4f else 0.15f
}

@Preview
@Composable
private fun NetworkImagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType = themeType) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            NetworkImage(
                imageUrl = null,
                imageRequestSize = 56.dp,
                modifier = Modifier.size(56.dp),
            )
            NetworkImage(
                imageUrl = null,
                imageRequestSize = 168.dp,
                modifier = Modifier.size(168.dp),
            )
        }
    }
}
