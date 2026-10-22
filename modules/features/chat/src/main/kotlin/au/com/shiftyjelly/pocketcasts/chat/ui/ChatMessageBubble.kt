package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
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
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AiMessageBubble(
    text: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    wordProgress: (() -> Float)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val bulletIndent = remember(textMeasurer, density) {
        with(density) { textMeasurer.measure(BULLET_PREFIX, AiMessageTextStyle).size.width.toSp() }
    }
    val (styledText, words) = remember(text, bulletIndent) { text.withHangingBullets(bulletIndent) }
    Text(
        text = wordProgress?.let { progress -> styledText.revealed(words, progress(), theme.aiBubbleText) } ?: styledText,
        color = theme.aiBubbleText,
        style = AiMessageTextStyle,
        modifier = modifier.fillMaxWidth(),
    )
}

private fun String.withHangingBullets(indent: TextUnit): Pair<AnnotatedString, List<IntRange>> {
    val lines = lines()
    val words = mutableListOf<IntRange>()
    val text = buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val isBullet = line.startsWith(BULLET_PREFIX)
            if (isBullet && lines.getOrNull(index - 1)?.startsWith(BULLET_PREFIX) == true) {
                withStyle(ParagraphStyle()) { append(ZERO_WIDTH_SPACE) }
            }
            val paragraphStyle = if (isBullet) ParagraphStyle(textIndent = TextIndent(restLine = indent)) else ParagraphStyle()
            words += wordRanges(line, startOffset = length)
            withStyle(paragraphStyle) { append(line.ifEmpty { ZERO_WIDTH_SPACE }) }
        }
    }
    return text to words
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
    isLoading: Boolean = false,
    wordProgress: (() -> Float)? = null,
    onClickPlay: () -> Unit = {},
) {
    val timestampColor = if (isPlayable) theme.quoteTimestamp else theme.secondaryText
    val quoteColor = if (isLoading) theme.aiBubbleText.copy(alpha = LOADING_QUOTE_ALPHA) else theme.aiBubbleText
    val text = remember(quote, timestampLabel, timestampColor, quoteColor, isLoading) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor)) {
                append(quote)
            }
            if (timestampLabel.isNotEmpty()) {
                append(" ")
                withStyle(SpanStyle(color = timestampColor)) {
                    append("($timestampLabel)")
                }
            }
            if (isLoading) {
                append(" ")
                appendInlineContent(QUOTE_SPINNER_ID, alternateText = " ")
            }
        }
    }
    val spinnerSize = with(LocalDensity.current) { QuoteSpinnerSize.toSp() }
    val inlineContent = remember(timestampColor, spinnerSize) {
        mapOf(
            QUOTE_SPINNER_ID to InlineTextContent(
                placeholder = Placeholder(width = spinnerSize, height = spinnerSize, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
            ) {
                QuoteLoadingSpinner(color = timestampColor)
            },
        )
    }
    val words = remember(text) { wordRanges(text.text) }
    val progress = wordProgress?.invoke()
    val isRevealed = progress == null || progress >= revealedWordTarget(words.size)
    val loadingDescription = stringResource(LR.string.chat_quote_finding_moment)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = progress?.let { text.revealed(words, it, theme.aiBubbleText) } ?: text,
            inlineContent = inlineContent,
            color = theme.aiBubbleText,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isPlayable && isRevealed) {
                        Modifier.clickable(
                            onClickLabel = stringResource(LR.string.chat_play_quote),
                            role = Role.Button,
                            onClick = onClickPlay,
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (isLoading) {
                        Modifier.semantics { stateDescription = loadingDescription }
                    } else {
                        Modifier
                    },
                ),
        )
        QuoteFindingMoment(
            isLoading = isLoading,
            theme = theme,
        )
    }
}

@Composable
private fun QuoteLoadingSpinner(
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (rememberIsReduceMotionEnabled()) {
        CircularProgressIndicator(
            progress = REDUCED_MOTION_SPINNER_PROGRESS,
            color = color,
            strokeWidth = 1.5.dp,
            modifier = modifier.size(QuoteSpinnerSize),
        )
    } else {
        CircularProgressIndicator(
            color = color,
            strokeWidth = 1.5.dp,
            modifier = modifier.size(QuoteSpinnerSize),
        )
    }
}

@Composable
private fun QuoteFindingMoment(
    isLoading: Boolean,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        isVisible = false
        if (isLoading) {
            delay(FINDING_MOMENT_DELAY_MS)
            isVisible = true
        }
    }
    if (isLoading && isVisible) {
        Text(
            text = stringResource(LR.string.chat_quote_finding_moment),
            color = theme.inputHint,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = modifier.clearAndSetSemantics {},
        )
    }
}

private const val QUOTE_SPINNER_ID = "quoteSpinner"
private val QuoteSpinnerSize = 12.dp
private const val LOADING_QUOTE_ALPHA = 0.6f
private const val REDUCED_MOTION_SPINNER_PROGRESS = 0.75f
private const val FINDING_MOMENT_DELAY_MS = 1_000L

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = if (isNotSent) {
                Modifier.clickable(
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
    transition: InfiniteTransition = rememberInfiniteTransition(label = "typing"),
) {
    ChatTypingIndicator(
        theme = theme,
        modifier = modifier,
        transition = transition,
    )
}

private val UserBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp)
