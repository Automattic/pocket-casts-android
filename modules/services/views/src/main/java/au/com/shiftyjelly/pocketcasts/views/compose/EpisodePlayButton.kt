package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonType
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.EventHorizon
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodePlayButton(
    episode: BaseEpisode,
    buttonType: PlayButtonType,
    tint: Color,
    listener: PlayButton.OnClickListener,
    eventHorizon: EventHorizon,
    modifier: Modifier = Modifier,
    fromListUuid: String? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isMenuExpanded by remember { mutableStateOf(false) }
    val buttonColor = if (buttonType == PlayButtonType.PLAYED) MaterialTheme.theme.colors.primaryIcon02 else tint
    val topPadding = with(LocalDensity.current) { 1.dp.toPx().toInt().toDp() }
    Box(
        contentAlignment = TruncatedCenter,
        modifier = modifier
            .width(52.dp)
            .semantics { contentDescription = buttonType.label }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = {
                    when (buttonType) {
                        PlayButtonType.PLAY -> {
                            val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
                            if (fromListUuid != null && podcastUuid != null) {
                                eventHorizon.track(DiscoverListEpisodePlayEvent(listId = fromListUuid, podcastUuid = podcastUuid))
                            }
                            listener.onPlayClicked(episode.uuid)
                            keyboardController?.hide()
                        }

                        PlayButtonType.PAUSE -> listener.onPauseClicked()

                        PlayButtonType.DOWNLOAD -> listener.onDownload(episode.uuid)

                        PlayButtonType.STOP_DOWNLOAD -> listener.onStopDownloading(episode.uuid)

                        PlayButtonType.PLAYBACK_FAILED -> listener.onPlayClicked(episode.uuid)

                        PlayButtonType.PLAYED -> listener.onPlayedClicked(episode.uuid)
                    }
                },
                onLongClick = { isMenuExpanded = true },
            )
            .padding(top = topPadding),
    ) {
        DrawableIcon(
            drawableId = buttonType.drawableId,
            tint = buttonColor,
            modifier = Modifier.size(32.dp),
        )
        ProgressCircle(
            state = ProgressCircleState.forEpisode(episode, isPlayed = buttonType == PlayButtonType.PLAYED),
            color = buttonColor,
            modifier = Modifier.size(32.dp),
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            PlayButtonMenuItem.visibleItems(episode.downloadStatus, buttonType).forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        isMenuExpanded = false
                        when (item) {
                            PlayButtonMenuItem.PlayNext -> listener.onPlayNext(episode.uuid)
                            PlayButtonMenuItem.PlayLast -> listener.onPlayLast(episode.uuid)
                            PlayButtonMenuItem.Download -> listener.onDownload(episode.uuid)
                            PlayButtonMenuItem.Stream -> listener.onPlayClicked(episode.uuid)
                            PlayButtonMenuItem.StopDownloading -> listener.onStopDownloading(episode.uuid)
                        }
                    },
                ) {
                    Text(text = stringResource(item.titleId))
                }
            }
        }
    }
}

private val TruncatedCenter = Alignment { size, space, _ ->
    IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2)
}

@Preview
@Composable
private fun EpisodePlayButtonPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Row {
            PlayButtonType.entries.forEach { type ->
                EpisodePlayButton(
                    episode = PodcastEpisode(
                        uuid = type.name,
                        publishedDate = Date(),
                        duration = 100.0,
                        playedUpTo = 30.0,
                        playingStatus = if (type == PlayButtonType.PAUSE) EpisodePlayingStatus.IN_PROGRESS else EpisodePlayingStatus.NOT_PLAYED,
                    ),
                    buttonType = type,
                    tint = MaterialTheme.theme.colors.primaryInteractive01,
                    listener = PreviewPlayButtonListener,
                    eventHorizon = EventHorizon {},
                    modifier = Modifier.height(64.dp),
                )
            }
        }
    }
}

private object PreviewPlayButtonListener : PlayButton.OnClickListener {
    override var source = SourceView.UNKNOWN
    override fun onPlayClicked(episodeUuid: String) = Unit
    override fun onPauseClicked() = Unit
    override fun onPlayNext(episodeUuid: String) = Unit
    override fun onPlayLast(episodeUuid: String) = Unit
    override fun onDownload(episodeUuid: String) = Unit
    override fun onStopDownloading(episodeUuid: String) = Unit
    override fun onPlayedClicked(episodeUuid: String) = Unit
}
