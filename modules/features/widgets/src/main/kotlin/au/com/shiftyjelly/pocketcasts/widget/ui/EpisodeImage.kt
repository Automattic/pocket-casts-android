package au.com.shiftyjelly.pocketcasts.widget.ui

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun EpisodeImage(
    episode: PlayerWidgetEpisode?,
    useEpisodeArtwork: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    var provider by remember(episode?.uuid, useEpisodeArtwork) {
        mutableStateOf(ImageProvider(IR.drawable.defaultartwork_small_rounded))
    }

    if (episode != null) {
        LaunchedEffect(episode.uuid, useEpisodeArtwork) {
            val requestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 16).smallSize()
            val request = requestFactory.create(episode.toBaseEpisode(), useEpisodeArtwork)
            var drawable: Drawable? = null
            while (drawable == null) {
                drawable = context.imageLoader.execute(request).drawable
            }
            val bitmap = withContext(Dispatchers.Default) { drawable.toBitmap() }
            provider = ImageProvider(bitmap)
        }
    }

    Image(
        provider = provider,
        contentDescription = null,
        modifier = modifier,
    )
}
