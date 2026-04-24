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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AiMessageBubble(
    text: String,
    podcastUuid: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        PodcastImage(
            uuid = podcastUuid,
            imageSize = 28.dp,
            cornerSize = 14.dp,
            elevation = null,
        )
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
internal fun AiQuoteBubble(
    quote: String,
    metadata: String?,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Keep the quote aligned with the assistant text bubble (below the avatar column).
        Box(modifier = Modifier.width(28.dp))
        Row(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(QuoteCardShape)
                .background(theme.aiBubble)
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
                    text = "“${quote.trim('"', '“', '”', ' ')}”",
                    color = theme.aiBubbleText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontStyle = FontStyle.Italic,
                )
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        color = theme.secondaryText,
                        fontSize = 13.sp,
                    )
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
    podcastUuid: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        PodcastImage(
            uuid = podcastUuid,
            imageSize = 28.dp,
            cornerSize = 14.dp,
            elevation = null,
        )
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
