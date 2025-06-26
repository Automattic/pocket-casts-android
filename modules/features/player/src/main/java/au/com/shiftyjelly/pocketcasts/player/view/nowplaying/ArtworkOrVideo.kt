package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoView
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import okhttp3.HttpUrl

@Composable
internal fun ArtworkOrVideo(
    state: ArtworkOrVideoState,
    onChapterUrlClick: (HttpUrl) -> Unit,
    configureVideoView: (VideoView) -> Unit,
    modifier: Modifier = Modifier,
    artworkCornerRadius: Dp = 16.dp,
) {
    Box(
        modifier = modifier,
    ) {
        when (state) {
            is ArtworkOrVideoState.Artwork -> {
                ArtworkImage(
                    state = state.artworkImageState,
                    cornerRadius = artworkCornerRadius,
                )
            }

            is ArtworkOrVideoState.Video -> {
                VideoBox(
                    player = state.player,
                    configureVideoView = configureVideoView,
                )
            }

            is ArtworkOrVideoState.NoContent -> Unit
        }
        val chapterUrl = state.chapterUrl
        if (chapterUrl != null) {
            ChapterLinkButton(
                onClick = { onChapterUrlClick(chapterUrl) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
            )
        }
    }
}

sealed interface ArtworkOrVideoState {
    val chapterUrl: HttpUrl?

    data class Artwork(
        val artworkImageState: ArtworkImageState,
        override val chapterUrl: HttpUrl?,
    ) : ArtworkOrVideoState

    data class Video(
        val player: Player?,
        override val chapterUrl: HttpUrl?,
    ) : ArtworkOrVideoState

    data object NoContent : ArtworkOrVideoState {
        override val chapterUrl get() = null
    }
}
