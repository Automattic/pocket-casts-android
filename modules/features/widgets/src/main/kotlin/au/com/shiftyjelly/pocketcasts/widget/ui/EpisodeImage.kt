package au.com.shiftyjelly.pocketcasts.widget.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun EpisodeImage(
    episode: PlayerWidgetEpisode?,
    useEpisodeArtwork: Boolean,
    size: Dp,
    backgroundColor: ((WidgetTheme) -> ColorProvider)? = null,
    onClick: Action? = null,
) {
    var episodeBitmap by remember(episode?.uuid, useEpisodeArtwork, size) {
        mutableStateOf<Bitmap?>(null)
    }

    RounderCornerBox(
        contentAlignment = Alignment.Center,
        backgroundTint = backgroundColor?.invoke(LocalWidgetTheme.current) ?: LocalWidgetTheme.current.buttonBackground,
        modifier = GlanceModifier.size(size).applyIf(onClick != null) { it.clickable(onClick!!) },
    ) {
        PocketCastsLogo(
            size = size / 2.5f,
        )

        val bitmap = episodeBitmap
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
            )
        }
    }

    if (episode != null) {
        val context = LocalContext.current
        LaunchedEffect(episode.uuid, useEpisodeArtwork, size) {
            val requestFactory = PocketCastsImageRequestFactory(
                context,
                size = size.value.toInt(),
                cornerRadius = if (isSystemCornerRadiusSupported) 0 else 6,
            )
            val request = requestFactory.create(episode.toBaseEpisode(), useEpisodeArtwork)
            var drawable: Drawable? = null
            while (drawable == null) {
                drawable = context.imageLoader.execute(request).drawable
            }
            episodeBitmap = withContext(Dispatchers.Default) { drawable.toBitmap() }
        }
    }
}
