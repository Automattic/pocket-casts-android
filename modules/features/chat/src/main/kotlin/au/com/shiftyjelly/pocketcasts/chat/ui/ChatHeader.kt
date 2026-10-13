package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.chat.ChatPlayback
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatHeader(
    episodeTitle: String,
    podcastUuid: String,
    podcastTitle: String,
    playback: ChatPlayback,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onClickPlayPause: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(theme.closeButtonBackground)
                    .clickable(role = Role.Button, onClick = onClickClose),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(LR.string.close),
                    tint = theme.iconButton,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClickMore) {
                Icon(
                    painter = painterResource(IR.drawable.ic_more_vert_black_24dp),
                    contentDescription = stringResource(LR.string.more_options),
                    tint = theme.iconButton,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        ) {
            PodcastImage(
                uuid = podcastUuid,
                imageSize = 44.dp,
                cornerSize = 4.dp,
                elevation = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = episodeTitle,
                    color = theme.primaryText,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = podcastTitle,
                    color = theme.secondaryText,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClickPlayPause) {
                Icon(
                    painter = painterResource(if (playback.isPlaying) IR.drawable.ic_widget_pause else IR.drawable.ic_widget_play),
                    contentDescription = stringResource(if (playback.isPlaying) LR.string.pause else LR.string.play),
                    tint = theme.iconButton,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(theme.progressTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(playback.progress)
                    .height(2.dp)
                    .background(theme.progress),
            )
        }
    }
}

@Preview
@Composable
private fun ChatHeaderPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(Theme.ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val theme = rememberChatTheme()
            ChatHeader(
                episodeTitle = "Navigating Life Insurance Choices",
                podcastUuid = "preview-podcast-uuid",
                podcastTitle = "Smart Money",
                playback = ChatPlayback(isPlaying = true, positionMs = 30, durationMs = 100),
                onClickClose = {},
                onClickMore = {},
                onClickPlayPause = {},
                theme = theme,
                modifier = Modifier.background(theme.background),
            )
        }
    }
}
