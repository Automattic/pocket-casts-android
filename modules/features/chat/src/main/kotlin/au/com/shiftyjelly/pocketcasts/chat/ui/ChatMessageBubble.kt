package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatContextBubble(
    episodeDurationMs: Int,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val durationLabel = if (episodeDurationMs > 0) {
        TimeHelper.getTimeDurationShortString(
            timeMs = episodeDurationMs.toLong(),
            context = context,
            emptyString = "",
        )
    } else {
        ""
    }
    Text(
        text = if (durationLabel.isEmpty()) {
            stringResource(LR.string.chat_context_label)
        } else {
            stringResource(LR.string.chat_context_label_with_duration, durationLabel)
        },
        color = theme.userBubble,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(theme.userBubble.copy(alpha = 0.12f), ChatContextShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
internal fun AiMessageBubble(
    text: String,
    podcastUuid: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        AssistantMessageTitle(
            podcastUuid = podcastUuid,
            theme = theme,
        )
        Text(
            text = text,
            color = theme.aiBubbleText,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

@Composable
private fun AssistantMessageTitle(
    podcastUuid: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        PodcastImage(
            uuid = podcastUuid,
            imageSize = 16.dp,
            cornerSize = 8.dp,
            elevation = null,
        )
        Text(
            text = stringResource(LR.string.chat_episode_assistant),
            color = theme.userBubble,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
internal fun AiQuoteBubble(
    quote: String,
    timestampLabel: String,
    isPlayable: Boolean,
    isPlaying: Boolean,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    onClickPlay: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(QuoteCardShape)
                .background(theme.aiBubble)
                .then(if (isPlayable) Modifier.clickable(onClick = onClickPlay) else Modifier)
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(theme.userBubble),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = quote,
                    color = theme.aiBubbleText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontStyle = FontStyle.Italic,
                )
                if (timestampLabel.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isPlayable) {
                            if (isPlaying) {
                                Icon(
                                    painter = painterResource(IR.drawable.ic_stop),
                                    contentDescription = stringResource(LR.string.chat_stop_quote),
                                    tint = theme.secondaryText,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(LR.string.chat_play_quote),
                                    tint = theme.secondaryText,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(
                            text = timestampLabel,
                            color = theme.secondaryText,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun UserMessageBubble(
    text: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    allowRetry: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.fillMaxWidth(),
    ) {
        val bubbleModifier = Modifier
            .widthIn(max = 300.dp)
            .then(if (allowRetry) Modifier.clickable(onClick = onRetry) else Modifier)
            .background(theme.userBubble, UserBubbleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
        Text(
            text = text,
            color = theme.userBubbleText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = bubbleModifier,
        )
        if (allowRetry) {
            Text(
                text = stringResource(LR.string.chat_tap_to_retry),
                color = theme.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
internal fun ThinkingBubble(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(LR.string.chat_thinking),
            color = theme.userBubble,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        ChatTypingIndicator(
            theme = theme,
            showBubble = false,
            dotColor = theme.secondaryText,
        )
    }
}

private val ChatContextShape = RoundedCornerShape(20.dp)

private val QuoteCardShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 16.dp,
)

private val UserBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 4.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)
