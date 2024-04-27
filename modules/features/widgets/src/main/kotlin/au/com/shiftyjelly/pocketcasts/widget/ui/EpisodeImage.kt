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
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.widget.action.OpenEpisodeDetailsAction
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
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
            .background(backgroundColor?.invoke(LocalWidgetTheme.current) ?: LocalWidgetTheme.current.buttonBackground)
            .cornerRadiusCompat(6.dp)
            .size(size),
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
        LaunchedEffect(episode.uuid, useEpisodeArtwork) {
            val requestFactory = PocketCastsImageRequestFactory(context).smallSize()
            val request = requestFactory.create(episode.toBaseEpisode(), useEpisodeArtwork)
            var drawable: Drawable? = null
            while (drawable == null) {
                drawable = context.imageLoader.execute(request).drawable
            }
            episodeBitmap = withContext(Dispatchers.Default) { drawable.toBitmap() }
        }
    }
}
