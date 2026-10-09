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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
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
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    onClickPlay: () -> Unit = {},
) {
    val timestampColor = if (isPlayable) theme.quoteTimestamp else theme.secondaryText
    val text = remember(quote, timestampLabel, timestampColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(quote)
            }
            if (timestampLabel.isNotEmpty()) {
                append(" ")
                withStyle(SpanStyle(color = timestampColor)) {
                    append("($timestampLabel)")
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
                    Modifier.clickable(
                        onClickLabel = stringResource(LR.string.chat_play_quote),
                        role = Role.Button,
                        onClick = onClickPlay,
                    )
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
