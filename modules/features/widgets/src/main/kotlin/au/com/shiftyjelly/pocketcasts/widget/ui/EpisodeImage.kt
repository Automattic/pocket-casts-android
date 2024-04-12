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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.widget.action.OpenEpisodeDetailsAction
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun EpisodeImage(
    episode: PlayerWidgetEpisode?,
    useEpisodeArtwork: Boolean,
    size: Dp?,
    backgroundColor: ((ColorProviders) -> ColorProvider)? = null,
    iconColor: ((ColorProviders) -> ColorProvider)? = null,
    onClick: (PlayerWidgetEpisode?) -> Action = { currentEpisode ->
        if (currentEpisode == null) {
            OpenPocketCastsAction.action()
        } else {
            OpenEpisodeDetailsAction.action(currentEpisode.uuid)
        }
    },
) {
    var episodeBitmap by remember(episode?.uuid, useEpisodeArtwork) {
        mutableStateOf<Bitmap?>(null)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .clickable(onClick(episode))
            .then(if (size != null) GlanceModifier.size(size) else GlanceModifier),
    ) {
        Image(
            provider = ImageProvider(IR.drawable.ic_rounded_square),
            contentDescription = null,
            colorFilter = ColorFilter.tint(backgroundColor?.invoke(GlanceTheme.colors) ?: GlanceTheme.colors.primary),
        )
        Image(
            provider = ImageProvider(IR.drawable.ic_logo_background),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconColor?.invoke(GlanceTheme.colors) ?: GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.padding(if (size == null) 12.dp else size / 6),
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
        LaunchedEffect(episode.uuid, useEpisodeArtwork) {
            val requestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 8).smallSize()
            val request = requestFactory.create(episode.toBaseEpisode(), useEpisodeArtwork)
            var drawable: Drawable? = null
            while (drawable == null) {
                drawable = context.imageLoader.execute(request).drawable
            }
            episodeBitmap = withContext(Dispatchers.Default) { drawable.toBitmap() }
        }
    }
}
