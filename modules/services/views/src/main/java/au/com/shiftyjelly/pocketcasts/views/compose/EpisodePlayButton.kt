package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonType
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
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
    val context = LocalContext.current
    val view = LocalView.current
    val currentEpisode by rememberUpdatedState(episode)
    val currentListener by rememberUpdatedState(listener)
    val anchor = remember { mutableStateOf<View?>(null) }
    val dragToOpenListener = remember { mutableStateOf<View.OnTouchListener?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val currentPress = remember { mutableStateOf<PressInteraction.Press?>(null) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            currentPress.value = (interaction as? PressInteraction.Press)
        }
    }
    val buttonColor = if (buttonType == PlayButtonType.PLAYED) MaterialTheme.theme.colors.primaryIcon02 else tint
    val topPadding = with(LocalDensity.current) { 1.dp.toPx().toInt().toDp() }
    Box(
        modifier = modifier
            .width(52.dp)
            .semantics { contentDescription = buttonType.label }
            .forwardTouches(anchor, dragToOpenListener) {
                currentPress.value?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false),
                onClick = {
                    when (buttonType) {
                        PlayButtonType.PLAY -> {
                            val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
                            if (fromListUuid != null && podcastUuid != null) {
                                eventHorizon.track(DiscoverListEpisodePlayEvent(listId = fromListUuid, podcastUuid = podcastUuid))
                            }
                            listener.onPlayClicked(episode.uuid)
                            UiUtil.hideKeyboard(view)
                        }

                        PlayButtonType.PAUSE -> listener.onPauseClicked()

                        PlayButtonType.DOWNLOAD -> listener.onDownload(episode.uuid)

                        PlayButtonType.STOP_DOWNLOAD -> listener.onStopDownloading(episode.uuid)

                        PlayButtonType.PLAYBACK_FAILED -> listener.onPlayClicked(episode.uuid)

                        PlayButtonType.PLAYED -> listener.onPlayedClicked(episode.uuid)
                    }
                },
                onLongClick = {
                    anchor.value?.let { anchorView ->
                        val popup = PopupMenu(context, anchorView)
                        dragToOpenListener.value = popup.dragToOpenListener
                        popup.inflate(R.menu.play_button)
                        val visibleItems = PlayButtonMenuItem.visibleItems(episode.downloadStatus, buttonType)
                        PlayButtonMenuItem.entries.forEach { item ->
                            popup.menu.findItem(item.menuId).isVisible = item in visibleItems
                        }
                        popup.setOnMenuItemClickListener { menuItem ->
                            val uuid = currentEpisode.uuid
                            when (PlayButtonMenuItem.entries.firstOrNull { it.menuId == menuItem.itemId }) {
                                PlayButtonMenuItem.PlayNext -> currentListener.onPlayNext(uuid)
                                PlayButtonMenuItem.PlayLast -> currentListener.onPlayLast(uuid)
                                PlayButtonMenuItem.Download -> currentListener.onDownload(uuid)
                                PlayButtonMenuItem.Stream -> currentListener.onPlayClicked(uuid)
                                PlayButtonMenuItem.StopDownloading -> currentListener.onStopDownloading(uuid)
                                null -> Unit
                            }
                            true
                        }
                        popup.show()
                    }
                },
            ),
    ) {
        AndroidView(
            factory = { viewContext -> PopupAnchorView(viewContext).also { anchor.value = it } },
            modifier = Modifier.matchParentSize(),
        )
        Box(
            contentAlignment = TruncatedCenter,
            modifier = Modifier
                .matchParentSize()
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
        }
    }
}

private fun Modifier.forwardTouches(
    anchor: State<View?>,
    touchListener: State<View.OnTouchListener?>,
    onForwardingStarted: () -> Unit,
) = pointerInput(Unit) {
    awaitPointerEventScope {
        var downTime = 0L
        var isForwarding = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.first()
            if (!change.pressed && !change.previousPressed) continue
            val action = when {
                change.changedToDownIgnoreConsumed() -> MotionEvent.ACTION_DOWN.also {
                    downTime = change.uptimeMillis
                    isForwarding = false
                }

                change.changedToUpIgnoreConsumed() -> MotionEvent.ACTION_UP

                else -> MotionEvent.ACTION_MOVE
            }
            val anchorView = anchor.value ?: continue
            val onTouch = touchListener.value ?: continue
            val motionEvent = MotionEvent.obtain(downTime, change.uptimeMillis, action, change.position.x, change.position.y, 0)
            if (onTouch.onTouch(anchorView, motionEvent)) {
                if (!isForwarding) onForwardingStarted()
                isForwarding = true
                event.changes.forEach { it.consume() }
            }
            motionEvent.recycle()
        }
    }
}

private class PopupAnchorView(context: Context) : View(context) {
    init {
        isLongClickable = true
    }

    override fun dispatchTouchEvent(event: MotionEvent) = false
}

private val PlayButtonMenuItem.menuId
    get() = when (this) {
        PlayButtonMenuItem.PlayNext -> R.id.play_next
        PlayButtonMenuItem.PlayLast -> R.id.play_last
        PlayButtonMenuItem.Download -> R.id.download
        PlayButtonMenuItem.Stream -> R.id.stream
        PlayButtonMenuItem.StopDownloading -> R.id.stop_downloading
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
