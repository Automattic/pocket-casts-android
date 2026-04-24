package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AiMessageBubble(
    text: String,
    podcastUuid: String?,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (podcastUuid != null) {
            PodcastImage(
                uuid = podcastUuid,
                imageSize = 28.dp,
                cornerSize = 14.dp,
                elevation = null,
            )
        }
        Text(
            text = text.formatAiBullets(),
            color = theme.aiBubbleText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(theme.aiBubble, AiBubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
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
    podcastUuid: String?,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (podcastUuid != null) {
            PodcastImage(
                uuid = podcastUuid,
                imageSize = 28.dp,
                cornerSize = 14.dp,
                elevation = null,
            )
        }
        ChatTypingIndicator(theme = theme)
    }
}

private fun String.formatAiBullets(): String {
    val bulletRegex = Regex("""(?m)^\s*[-*]\s+""")
    return bulletRegex.replace(this, "•  ")
}

private val AiBubbleShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)

private val UserBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 4.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)
