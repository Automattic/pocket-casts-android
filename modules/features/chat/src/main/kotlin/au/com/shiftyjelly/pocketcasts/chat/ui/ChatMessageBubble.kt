package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AiMessageBubble(
    text: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = theme.aiBubbleText,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        modifier = modifier.fillMaxWidth(),
    )
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
    val currentOnClickPlay by rememberUpdatedState(onClickPlay)
    val actionLabel = stringResource(if (isPlaying) LR.string.chat_stop_quote else LR.string.chat_play_quote)
    val timestampColor = if (isPlayable) theme.quoteTimestamp else theme.secondaryText
    val text = remember(quote, timestampLabel, isPlayable, isPlaying, timestampColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(quote)
            }
            if (timestampLabel.isNotEmpty()) {
                append(" ")
                val timestampStyle = SpanStyle(
                    color = timestampColor,
                    textDecoration = if (isPlaying) TextDecoration.Underline else null,
                )
                if (isPlayable) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = QUOTE_TIMESTAMP_TAG,
                            styles = TextLinkStyles(timestampStyle),
                            linkInteractionListener = { currentOnClickPlay() },
                        ),
                    ) {
                        append("($timestampLabel)")
                    }
                } else {
                    withStyle(timestampStyle) {
                        append("($timestampLabel)")
                    }
                }
            }
        }
    }
    Text(
        text = text,
        color = theme.aiBubbleText,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isPlayable) {
                    Modifier.clearAndSetSemantics {
                        contentDescription = text.text
                        onClick(label = actionLabel) {
                            currentOnClickPlay()
                            true
                        }
                    }
                } else {
                    Modifier
                },
            ),
    )
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
            .widthIn(max = 250.dp)
            .clip(UserBubbleShape)
            .then(if (allowRetry) Modifier.clickable(onClick = onRetry) else Modifier)
            .background(theme.userBubble)
            .padding(horizontal = 16.dp, vertical = 10.dp)
        Text(
            text = text,
            color = theme.userBubbleText,
            fontSize = 15.sp,
            lineHeight = 20.sp,
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
    ChatTypingIndicator(
        theme = theme,
        showBubble = false,
        dotColor = theme.secondaryText,
        modifier = modifier,
    )
}

private val UserBubbleShape = RoundedCornerShape(16.dp)

private const val QUOTE_TIMESTAMP_TAG = "quote_timestamp"
