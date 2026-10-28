package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AiMessageBubble(
    text: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    revealProgress: (() -> Float)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val bulletIndent = remember(textMeasurer, density) {
        with(density) { textMeasurer.measure(BULLET_PREFIX, AiMessageTextStyle).size.width.toSp() }
    }
    val styledText = remember(text, bulletIndent) { text.withHangingBullets(bulletIndent) }
    val wipe = remember { TextWipe() }
    Text(
        text = styledText,
        color = theme.aiBubbleText,
        style = AiMessageTextStyle,
        onTextLayout = wipe::onTextLayout,
        modifier = modifier
            .fillMaxWidth()
            .revealedBy(wipe, styledText, revealProgress),
    )
}

@Composable
private fun Modifier.revealedBy(
    wipe: TextWipe,
    text: AnnotatedString,
    revealProgress: (() -> Float)?,
): Modifier {
    if (revealProgress == null) return this
    val timeline = remember(text) { TextRevealTimeline(text) }
    return textWipe(wipe) { timeline.revealedLength(revealProgress()) }
}

internal fun String.withHangingBullets(indent: TextUnit): AnnotatedString {
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
internal const val ZERO_WIDTH_SPACE = "\u200B"

private val AiMessageTextStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)

@Composable
internal fun AiQuoteBubble(
    quote: String,
    timestampLabel: String,
    isPlayable: Boolean,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    revealProgress: (() -> Float)? = null,
    onClickPlay: () -> Unit = {},
) {
    val timestampColor = if (isPlayable) theme.quoteTimestamp else theme.secondaryText
    val quoteColor = if (isLoading) theme.aiBubbleText.copy(alpha = LOADING_QUOTE_ALPHA) else theme.aiBubbleText
    val text = remember(quote, timestampLabel, timestampColor, quoteColor, isLoading) {
        quoteText(quote, timestampLabel, quoteColor, timestampColor, isLoading)
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
    val wipe = remember { TextWipe() }
    val revealTicks = remember(text) { TextRevealTimeline(text).ticks }
    val isRevealed by remember(revealProgress, revealTicks) {
        derivedStateOf { revealProgress == null || revealProgress() >= revealTicks }
    }
    val loadingDescription = stringResource(LR.string.chat_quote_finding_moment)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            inlineContent = inlineContent,
            onTextLayout = wipe::onTextLayout,
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
                .revealedBy(wipe, text, revealProgress)
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

internal fun quoteTimestampText(timestampLabel: String) = if (timestampLabel.isEmpty()) "" else "($timestampLabel)"

internal fun quoteText(
    quote: String,
    timestampLabel: String,
    quoteColor: Color = Color.Unspecified,
    timestampColor: Color = Color.Unspecified,
    isLoading: Boolean = false,
) = buildAnnotatedString {
    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor)) {
        append(quote)
    }
    if (timestampLabel.isNotEmpty()) {
        append(" ")
        withStyle(SpanStyle(color = timestampColor)) {
            append(quoteTimestampText(timestampLabel))
        }
    }
    if (isLoading) {
        append(" ")
        appendInlineContent(QUOTE_SPINNER_ID, alternateText = " ")
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
    chipMorph: () -> Float = { 0f },
) {
    val isChipWidth by remember(chipMorph) { derivedStateOf { chipMorph() >= CHIP_WIDTH_MORPH_POINT } }
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
                    .widthIn(max = if (isChipWidth) Dp.Unspecified else UserBubbleMaxWidth)
                    .drawBehind { drawUserBubble(chipMorph(), theme) }
                    .padding(horizontal = 16.dp)
                    .verticalPadding { lerp(10.dp, 12.dp, chipMorph()) },
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
    phase: ChatLoadingPhase,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    transition: InfiniteTransition = rememberInfiniteTransition(label = "typing"),
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            if (isReduceMotionEnabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                LoadingPhaseEnter togetherWith LoadingPhaseExit using SizeTransform(clip = false)
            }
        },
        contentAlignment = Alignment.TopStart,
        label = "loadingPhase",
        modifier = modifier.fillMaxWidth(),
    ) { shownPhase ->
        val textRes = shownPhase.textRes
        if (textRes == null) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.heightIn(min = LoadingLineHeight),
            ) {
                ChatTypingIndicator(
                    theme = theme,
                    transition = transition,
                )
            }
        } else {
            ChatShimmerText(
                text = stringResource(textRes),
                color = theme.inputHint,
                highlight = theme.primaryText,
                style = AiMessageTextStyle,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

private val LoadingLineHeight = 20.dp
private val LoadingPhaseEnter = fadeIn(tween(durationMillis = 250, delayMillis = 150)) +
    slideInVertically(tween(durationMillis = 250, delayMillis = 150, easing = EaseOut)) { height -> height / 4 }
private val LoadingPhaseExit = fadeOut(tween(durationMillis = 150)) +
    slideOutVertically(tween(durationMillis = 150, easing = EaseOut)) { height -> -height * 3 / 20 }

private val UserBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp)

private fun DrawScope.drawUserBubble(chipMorph: Float, theme: ChatTheme) {
    val shape = if (chipMorph == 0f) UserBubbleShape else chipMorphShape(chipMorph)
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        color = theme.userBubble.copy(alpha = theme.userBubble.alpha * (1f - chipMorph)),
    )
    if (chipMorph > 0f) {
        val strokeWidth = ChipBorderWidth.toPx()
        inset(strokeWidth / 2) {
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                color = theme.chipBorder.copy(alpha = theme.chipBorder.alpha * chipMorph),
                style = Stroke(strokeWidth),
            )
        }
    }
}

private fun Modifier.verticalPadding(padding: Density.() -> Dp) = layout { measurable, constraints ->
    val vertical = padding().roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = -vertical * 2))
    layout(placeable.width, constraints.constrainHeight(placeable.height + vertical * 2)) {
        placeable.place(0, vertical)
    }
}

private fun chipMorphShape(chipMorph: Float) = RoundedCornerShape(
    topStart = lerp(16.dp, ChipCornerRadius, chipMorph),
    topEnd = lerp(16.dp, ChipCornerRadius, chipMorph),
    bottomEnd = lerp(4.dp, ChipCornerRadius, chipMorph),
    bottomStart = lerp(16.dp, ChipCornerRadius, chipMorph),
)

private val ChipCornerRadius = 22.dp
private val ChipBorderWidth = 1.dp
private val UserBubbleMaxWidth = 250.dp
private const val CHIP_WIDTH_MORPH_POINT = 0.5f

@Preview
@Composable
private fun ThinkingBubblePreview(
    @PreviewParameter(ChatLoadingPhaseParameterProvider::class) phase: ChatLoadingPhase,
) {
    AppThemeWithBackground(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides PodcastColors.ForUserEpisode) {
            val theme = rememberChatTheme()
            ThinkingBubble(
                phase = phase,
                theme = theme,
                modifier = Modifier
                    .background(theme.background)
                    .padding(16.dp),
            )
        }
    }
}

private class ChatLoadingPhaseParameterProvider : PreviewParameterProvider<ChatLoadingPhase> {
    override val values = ChatLoadingPhase.entries.asSequence()
}
