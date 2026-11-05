package au.com.shiftyjelly.pocketcasts.views.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.episode.EpisodePlayButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeRowPlayButton(
    episode: BaseEpisode,
    buttonType: PlayButtonType,
    listener: PlayButton.OnClickListener,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.theme.colors.primaryIcon01,
) {
    val view = LocalView.current
    var isMenuShown by remember { mutableStateOf(false) }
    Box(
        modifier = modifier,
    ) {
        EpisodePlayButton(
            iconId = buttonType.drawableId,
            contentDescription = buttonType.label,
            progress = when {
                buttonType == PlayButtonType.PLAYED -> null
                episode.isInProgress && episode.durationMs > 0 -> 1f - episode.playedUpToMs.toFloat() / episode.durationMs
                else -> 1f
            },
            tint = if (buttonType == PlayButtonType.PLAYED) MaterialTheme.theme.colors.primaryIcon02 else tint,
            onClick = {
                when (buttonType) {
                    PlayButtonType.PLAY, PlayButtonType.PLAYBACK_FAILED -> {
                        listener.onPlayClicked(episode.uuid)
                        UiUtil.hideKeyboard(view)
                    }

                    PlayButtonType.PAUSE -> listener.onPauseClicked()

                    PlayButtonType.DOWNLOAD -> listener.onDownload(episode.uuid)

                    PlayButtonType.STOP_DOWNLOAD -> listener.onStopDownloading(episode.uuid)

                    PlayButtonType.PLAYED -> listener.onPlayedClicked(episode.uuid)
                }
            },
            onLongClick = { isMenuShown = true },
        )
        DropdownMenu(
            expanded = isMenuShown,
            onDismissRequest = { isMenuShown = false },
        ) {
            PlayButtonMenuItem.visibleItems(episode.downloadStatus, buttonType).forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        isMenuShown = false
                        item.onClick(listener, episode.uuid)
                    },
                ) {
                    Text(stringResource(item.title))
                }
            }
        }
    }
}

private val PlayButtonMenuItem.title
    get() = when (this) {
        PlayButtonMenuItem.PlayNext -> LR.string.play_next
        PlayButtonMenuItem.PlayLast -> LR.string.play_last
        PlayButtonMenuItem.Download -> LR.string.download
        PlayButtonMenuItem.Stream -> LR.string.stream
        PlayButtonMenuItem.StopDownloading -> LR.string.stop_downloading
    }

private fun PlayButtonMenuItem.onClick(listener: PlayButton.OnClickListener, episodeUuid: String) = when (this) {
    PlayButtonMenuItem.PlayNext -> listener.onPlayNext(episodeUuid)
    PlayButtonMenuItem.PlayLast -> listener.onPlayLast(episodeUuid)
    PlayButtonMenuItem.Download -> listener.onDownload(episodeUuid)
    PlayButtonMenuItem.Stream -> listener.onPlayClicked(episodeUuid)
    PlayButtonMenuItem.StopDownloading -> listener.onStopDownloading(episodeUuid)
}
