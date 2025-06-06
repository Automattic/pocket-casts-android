package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoView
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import coil.compose.rememberAsyncImagePainter
import okhttp3.HttpUrl

@Composable
internal fun PlayerVisuals(
    state: PlayerVisualsState,
    player: Player?,
    onChapterUrlClick: (HttpUrl) -> Unit,
    configureVideoView: (VideoView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoView = movableContentOf {
        if (LocalInspectionMode.current) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .background(Color(0xFFD5F6FB), RoundedCornerShape(12.dp))
                    .sizeIn(minWidth = 200.dp, minHeight = 112.5.dp),
            ) {
                Text(
                    text = "VideoView preview",
                    color = Color.Black,
                )
            }
        } else {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply(configureVideoView)
                },
                update = { videoView ->
                    videoView.player = player
                    videoView.updatePlayerPrepared(state.isPlaybackPrepared)
                    videoView.show = state.contentState is VisualContentState.DisplayVideo
                },
            )
        }
    }

    Box(
        modifier = modifier,
    ) {
        when (val contentState = state.contentState) {
            is VisualContentState.DisplayArtwork -> {
                ArtworkImage(
                    state = contentState,
                )
            }

            is VisualContentState.DisplayVideo -> {
                videoView()
            }

            is VisualContentState.NoContent -> Unit
        }
        val chapterUrl = state.contentState.chapterUrl
        if (chapterUrl != null) {
            ChapterButton(
                onClick = { onChapterUrlClick(chapterUrl) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun ArtworkImage(
    state: VisualContentState.DisplayArtwork,
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val factory = remember(context) {
        val placeholderType = if (isPreview) PlaceholderType.Large else PlaceholderType.None
        PocketCastsImageRequestFactory(context, placeholderType = placeholderType).themed()
    }
    val imageRequest = remember(state, factory) {
        if (state.chapterArtworkPath != null) {
            factory.createForFileOrUrl(state.chapterArtworkPath)
        } else {
            factory.create(state.episode, state.canDisplayEpisodeArtwork)
        }
    }

    Image(
        painter = rememberAsyncImagePainter(
            model = imageRequest,
            contentScale = ContentScale.Fit,
        ),
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
    )
}

@Composable
private fun ChapterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            Color.White,
            RippleDefaults.rippleAlpha(Color.White, lightTheme = true),
        ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier.clip(CircleShape),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_link_back),
                contentDescription = stringResource(id = au.com.shiftyjelly.pocketcasts.localization.R.string.player_chapter_url),
                modifier = Modifier.sizeIn(36.dp),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_link),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal data class PlayerVisualsState(
    val contentState: VisualContentState,
    val isPlaybackPrepared: Boolean,
) {
    companion object {
        val Empty = PlayerVisualsState(VisualContentState.NoContent, isPlaybackPrepared = false)
    }
}

internal sealed interface VisualContentState {
    val chapterUrl: HttpUrl?

    data class DisplayArtwork(
        val episode: BaseEpisode,
        val canDisplayEpisodeArtwork: Boolean,
        val chapterArtworkPath: String?,
        override val chapterUrl: HttpUrl?,
    ) : VisualContentState

    data class DisplayVideo(
        override val chapterUrl: HttpUrl?,
    ) : VisualContentState

    data object NoContent : VisualContentState {
        override val chapterUrl get() = null
    }
}
