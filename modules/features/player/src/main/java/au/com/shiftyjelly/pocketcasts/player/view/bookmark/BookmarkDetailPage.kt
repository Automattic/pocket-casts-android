package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRowColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonColors
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedPlayPauseButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun BookmarkDetailPage(
    displayTitle: String,
    aiSummary: String?,
    episodeTitle: String,
    episodeUuid: String,
    timeSecs: Int,
    createdAtText: String,
    playbackManager: PlaybackManager,
    sourceView: SourceView,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val onPlayClick: () -> Unit = {
        scope.launch {
            playbackManager.playNowSuspend(episodeUuid, sourceView = sourceView)
            playbackManager.seekToTimeMs(positionMs = timeSecs * 1000)
        }
    }
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()
    val colors = remember(theme.type, playerColors) {
        if (playerColors != null) {
            BookmarkRowColors.player(playerColors)
        } else {
            BookmarkRowColors.default(theme.colors)
        }
    }
    val playButtonColors = remember(theme.type, playerColors) {
        if (playerColors != null) {
            TimePlayButtonColors.player(playerColors)
        } else {
            TimePlayButtonColors.default(theme.colors)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        DragHandle(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
        )

        Header(
            episodeUuid = episodeUuid,
            timeSecs = timeSecs,
            playbackManager = playbackManager,
            sourceView = sourceView,
            buttonColor = colors.primaryText,
            buttonBackgroundColor = colors.primaryText.copy(alpha = 0.15f),
            onClose = onClose,
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (episodeTitle.isNotEmpty()) {
                TextH70(
                    text = episodeTitle,
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            TextH30(
                text = displayTitle,
                color = colors.primaryText,
            )

            Spacer(modifier = Modifier.height(12.dp))

            TimePlayButton(
                timeSecs = timeSecs,
                contentDescriptionId = LR.string.bookmark_play,
                onClick = onPlayClick,
                colors = playButtonColors,
            )

            if (!aiSummary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                TextP40(
                    text = aiSummary,
                    color = colors.secondaryText,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextH70(
                text = createdAtText,
                color = colors.secondaryText,
            )
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.theme.colors.primaryText01.copy(alpha = 0.3f)),
    )
}

@Composable
private fun Header(
    episodeUuid: String,
    timeSecs: Int,
    playbackManager: PlaybackManager,
    sourceView: SourceView,
    buttonColor: Color,
    buttonBackgroundColor: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val playbackState by remember {
        playbackManager.playbackStateFlow.map { it.episodeUuid to it.isPlaying }
    }.collectAsState(initial = null)
    val isPlayingThisEpisode = playbackState?.first == episodeUuid && playbackState?.second == true

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.offset(x = (-4).dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(LR.string.close),
                tint = buttonColor,
            )
        }

        AnimatedPlayPauseButton(
            isPlaying = isPlayingThisEpisode,
            onClick = {
                scope.launch {
                    if (isPlayingThisEpisode) {
                        playbackManager.pause(sourceView = sourceView)
                    } else {
                        playbackManager.playNowSuspend(episodeUuid, sourceView = sourceView)
                        playbackManager.seekToTimeMs(positionMs = timeSecs * 1000)
                    }
                }
            },
            iconWidth = 24.dp,
            iconHeight = 24.dp,
            circleSize = 48.dp,
            iconTint = buttonColor,
            circleColor = buttonBackgroundColor,
        )
    }
}
