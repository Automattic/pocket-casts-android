package au.com.shiftyjelly.pocketcasts.widget.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.widget.R
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.action.PausePlaybackAction
import au.com.shiftyjelly.pocketcasts.widget.action.ResumePlaybackAction
import au.com.shiftyjelly.pocketcasts.widget.action.SkipBackAction
import au.com.shiftyjelly.pocketcasts.widget.action.SkipForwardAction
import au.com.shiftyjelly.pocketcasts.widget.data.ClassicPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ClassicPlayer(state: ClassicPlayerWidgetState) {
    WidgetTheme(state.useDynamicColors) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .height(72.dp)
                .fillMaxWidth(),
        ) {
            BackgroundSurface(state.useDynamicColors)
            if (state.episode != null) {
                Content(state)
            } else {
                NoContent(state.useDynamicColors)
            }
        }
    }
}

@Composable
private fun BackgroundSurface(
    useDynamicColors: Boolean,
) {
    AndroidRemoteViews(
        remoteViews = RemoteViews(
            LocalContext.current.packageName,
            if (useDynamicColors) R.layout.classic_surface_dynamic else R.layout.classic_surface_default,
        ),
        modifier = GlanceModifier.fillMaxSize(),
    )
}

@Composable
private fun Content(
    state: ClassicPlayerWidgetState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(72.dp)
                .clickable(OpenPocketCastsAction.action())
                .semantics { contentDescription = "${state.episode?.title}. Open Pocket Casts" },
        ) {
            Cover(episode = state.episode, useEpisodeArtwork = state.useEpisodeArtwork)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .fillMaxHeight()
                .defaultWeight()
                .clickable(SkipBackAction.action(LocalSource.current))
                .semantics { contentDescription = "Skip back ${state.skipBackwardSeconds} seconds" },
        ) {
            NonScalingText(
                text = state.skipBackwardSeconds.toString(),
                textSize = 12.dp,
                useDynamicColors = state.useDynamicColors,
                nonDynamicTextColor = Color.White,
                modifier = GlanceModifier.padding(top = 4.dp, start = 2.dp),
            )
            Image(
                provider = ImageProvider(R.drawable.widget_classic_skip_backward),
                colorFilter = ColorFilter.tint(LocalWidgetTheme.current.iconClassic),
                contentDescription = null,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .fillMaxHeight()
                .defaultWeight()
                .clickable(if (state.isPlaying) PausePlaybackAction.action(LocalSource.current) else ResumePlaybackAction.action(LocalSource.current))
                .semantics { contentDescription = if (state.isPlaying) "Pause" else "Play" },
        ) {
            Image(
                provider = ImageProvider(if (state.isPlaying) R.drawable.widget_classic_pause else R.drawable.widget_classic_play),
                colorFilter = ColorFilter.tint(LocalWidgetTheme.current.iconClassic),
                contentDescription = null,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .fillMaxHeight()
                .defaultWeight()
                .clickable(SkipForwardAction.action(LocalSource.current))
                .semantics { contentDescription = "Skip forward ${state.skipForwardSeconds} seconds" },
        ) {
            NonScalingText(
                text = state.skipForwardSeconds.toString(),
                textSize = 12.dp,
                useDynamicColors = state.useDynamicColors,
                nonDynamicTextColor = Color.White,
                modifier = GlanceModifier.padding(top = 4.dp, end = 2.dp),
            )
            Image(
                provider = ImageProvider(R.drawable.widget_classic_skip_forward),
                colorFilter = ColorFilter.tint(LocalWidgetTheme.current.iconClassic),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun Cover(
    episode: PlayerWidgetEpisode?,
    useEpisodeArtwork: Boolean,
) {
    var episodeBitmap by remember(episode?.uuid, useEpisodeArtwork) {
        mutableStateOf<Bitmap?>(null)
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

    val bitmap = episodeBitmap
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier
                .size(72.dp)
                .padding(8.dp),
        )
    }
}

@Composable
private fun NoContent(
    useDynamicColors: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(OpenPocketCastsAction.action()),
    ) {
        AndroidRemoteViews(
            remoteViews = RemoteViews(
                LocalContext.current.packageName,
                if (useDynamicColors) R.layout.classic_no_content_dynamic else R.layout.classic_no_content_default,
            ),
            modifier = GlanceModifier.wrapContentSize(),
        )
    }
}
