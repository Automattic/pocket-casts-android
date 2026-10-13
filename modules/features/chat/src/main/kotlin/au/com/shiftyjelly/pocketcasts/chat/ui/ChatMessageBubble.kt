package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AiMessageBubble(
    text: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val bulletIndent = remember(textMeasurer, density) {
        with(density) { textMeasurer.measure(BULLET_PREFIX, AiMessageTextStyle).size.width.toSp() }
    }
    val styledText = remember(text, bulletIndent) { text.withHangingBullets(bulletIndent) }
    Text(
        text = styledText,
        color = theme.aiBubbleText,
        style = AiMessageTextStyle,
        modifier = modifier.fillMaxWidth(),
    )
}

private fun String.withHangingBullets(indent: TextUnit): AnnotatedString {
    val lines = lines()
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val isBullet = line.startsWith(BULLET_PREFIX)
            if (isBullet && lines.getOrNull(index - 1)?.startsWith(BULLET_PREFIX) == true) {
                withStyle(ParagraphStyle()) { append(ZERO_WIDTH_SPACE) }
            }
            val paragraphStyle = if (isBullet) ParagraphStyle(textIndent = TextIndent(restLine = indent)) else ParagraphStyle()
            withStyle(paragraphStyle) { append(line.ifEmpty { ZERO_WIDTH_SPACE }) }
        }
    }
}

private const val BULLET_PREFIX = "•  "
private const val ZERO_WIDTH_SPACE = "\u200B"

private val AiMessageTextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)

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
    isNotSent: Boolean = false,
    isRetryEnabled: Boolean = true,
    onRetry: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = if (isNotSent) {
                Modifier
                    .clip(UserBubbleShape)
                    .clickable(
                        enabled = isRetryEnabled,
                        onClickLabel = stringResource(LR.string.try_again),
                        role = Role.Button,
                        onClick = onRetry,
                    )
            } else {
                Modifier
            },
        ) {
            Text(
                text = text,
                color = theme.userBubbleText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .widthIn(max = 250.dp)
                    .clip(UserBubbleShape)
                    .background(theme.userBubble)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
            if (isNotSent) {
                ChatNotSentLabel(theme = theme)
            }
        }
    }
}

@Composable
private fun ChatNotSentLabel(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_retry),
            contentDescription = null,
            tint = theme.errorText,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(LR.string.chat_not_sent_tap_to_retry),
            color = theme.errorText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun ThinkingBubble(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    ChatTypingIndicator(
        theme = theme,
        modifier = modifier,
    )
}

private val UserBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp)
