package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatInputType
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatWelcome(
    starterQuestions: List<String>,
    isLoadingStarterQuestions: Boolean,
    isSuggestionEnabled: Boolean,
    onClickSuggestion: (text: String, inputType: ChatInputType, topInRoot: Float) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    exitingSuggestion: String? = null,
    isExiting: Boolean = false,
) {
    fun isTapped(text: String) = isExiting && text == exitingSuggestion
    val summarize = stringResource(LR.string.chat_suggestion_summarize)
    var isSkeletonExitRunning by remember { mutableStateOf(isLoadingStarterQuestions) }
    LaunchedEffect(isLoadingStarterQuestions) {
        if (isLoadingStarterQuestions) {
            isSkeletonExitRunning = true
        } else {
            delay(SKELETON_EXIT_MS)
            isSkeletonExitRunning = false
        }
    }
    val hasSkeletons = isLoadingStarterQuestions || isSkeletonExitRunning
    val sweepMillis = rememberSkeletonSweep(isRunning = hasSkeletons)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier.fillMaxWidth(),
    ) {
        WelcomeItem(order = 0, isExiting = isExiting, animate = animate, delayMillis = WELCOME_DELAY_MS) {
            Text(
                text = stringResource(LR.string.chat_welcome),
                color = theme.primaryText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            WelcomeItem(
                order = 1,
                isExiting = isExiting,
                animate = animate,
                delayMillis = WELCOME_DELAY_MS + WELCOME_STAGGER_MS,
                isHeld = isTapped(summarize),
            ) {
                ChatSuggestionChip(
                    text = summarize,
                    enabled = isSuggestionEnabled,
                    onClick = { topInRoot -> onClickSuggestion(summarize, ChatInputType.SummaryPrompt, topInRoot) },
                    theme = theme,
                    modifier = Modifier.hiddenIf(isTapped(summarize)),
                )
            }
            val slotCount = if (hasSkeletons) maxOf(SkeletonWidthFractions.size, starterQuestions.size) else starterQuestions.size
            repeat(slotCount) { index ->
                val question = starterQuestions.getOrNull(index)
                val chip: @Composable (String) -> Unit = { text ->
                    ChatSuggestionChip(
                        text = text,
                        enabled = isSuggestionEnabled,
                        onClick = { topInRoot -> onClickSuggestion(text, ChatInputType.SuggestedPrompt, topInRoot) },
                        theme = theme,
                        modifier = Modifier.hiddenIf(isTapped(text)),
                    )
                }
                if (isExiting) {
                    WelcomeItem(order = index + 2, isExiting = true, isHeld = question != null && isTapped(question)) {
                        if (question != null) {
                            chip(question)
                        } else {
                            ChatSkeletonChip(index = index, sweepMillis = sweepMillis, theme = theme)
                        }
                    }
                } else {
                    ChatStarterQuestionSlot(
                        index = index,
                        question = question,
                        isLoading = isLoadingStarterQuestions,
                        animate = animate,
                        sweepMillis = sweepMillis,
                        theme = theme,
                        chip = chip,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSkeletonSweep(isRunning: Boolean): () -> Float {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(isRunning, isReduceMotionEnabled) {
        if (!isRunning || isReduceMotionEnabled) return@LaunchedEffect
        while (true) {
            sweep.snapTo(0f)
            sweep.animateTo(SKELETON_SWEEP_PERIOD_MS.toFloat(), tween(SKELETON_SWEEP_PERIOD_MS, easing = LinearEasing))
        }
    }
    return remember(sweep) { { sweep.value } }
}

@Composable
private fun WelcomeItem(
    order: Int,
    isExiting: Boolean,
    animate: Boolean = true,
    delayMillis: Int = 0,
    isHeld: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (isHeld) {
        content()
    } else if (isExiting) {
        ChatFadeOut(
            durationMillis = WELCOME_EXIT_MS,
            delayMillis = order * WELCOME_EXIT_STAGGER_MS,
            easing = LinearEasing,
            content = content,
        )
    } else {
        ChatAppear(
            animate = animate,
            rise = WelcomeRise,
            durationMillis = WELCOME_APPEAR_MS,
            delayMillis = delayMillis,
            content = content,
        )
    }
}

private fun Modifier.hiddenIf(hidden: Boolean) = if (hidden) alpha(0f) else this

@Composable
private fun ChatStarterQuestionSlot(
    index: Int,
    question: String?,
    isLoading: Boolean,
    animate: Boolean,
    sweepMillis: () -> Float,
    theme: ChatTheme,
    chip: @Composable (String) -> Unit,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    var showsSkeleton by remember { mutableStateOf(question == null) }
    val questionAlpha = remember { Animatable(if (question == null) 0f else 1f) }
    val isSkeletonVisible by remember { derivedStateOf { questionAlpha.value < 1f } }
    LaunchedEffect(question != null, isReduceMotionEnabled) {
        when {
            question == null -> {
                showsSkeleton = true
                questionAlpha.snapTo(0f)
            }

            isReduceMotionEnabled -> questionAlpha.snapTo(1f)

            else -> questionAlpha.animateTo(
                1f,
                tween(
                    durationMillis = STARTER_CROSS_FADE_MS,
                    delayMillis = index * STARTER_CROSS_FADE_STAGGER_MS,
                    easing = LinearEasing,
                ),
            )
        }
    }
    AnimatedVisibility(
        visible = question != null || isLoading,
        enter = EnterTransition.None,
        exit = if (isReduceMotionEnabled) ExitTransition.None else fadeOut(tween(STARTER_CROSS_FADE_MS)),
    ) {
        ChatAppear(
            animate = animate,
            durationMillis = SKELETON_APPEAR_MS,
            delayMillis = SKELETON_DELAY_MS + index * SKELETON_STAGGER_MS,
        ) {
            Box(contentAlignment = Alignment.CenterEnd) {
                if (showsSkeleton && isSkeletonVisible) {
                    ChatSkeletonChip(
                        index = index,
                        sweepMillis = sweepMillis,
                        theme = theme,
                        modifier = Modifier.graphicsLayer { alpha = 1f - questionAlpha.value },
                    )
                }
                if (question != null) {
                    Box(modifier = Modifier.graphicsLayer { alpha = questionAlpha.value }) {
                        chip(question)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSkeletonChip(
    index: Int,
    sweepMillis: () -> Float,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val widthFraction = SkeletonWidthFractions.getOrElse(index) { SkeletonWidthFractions.last() }
    val lagMillis = if (index == 0) 0 else SKELETON_SWEEP_LAG_MS
    val density = LocalDensity.current
    val halfBand = with(density) { SkeletonHalfBand.toPx() }
    val speed = with(density) { SKELETON_SWEEP_DP_PER_MS.dp.toPx() }
    val fill = theme.inputBackground.copy(alpha = theme.inputBackground.alpha * SKELETON_FILL_ALPHA)
    val highlight = theme.primaryText.copy(alpha = SKELETON_HIGHLIGHT_ALPHA)
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(SkeletonHeight)
            .clip(CircleShape)
            .drawBehind {
                drawRect(fill)
                if (!isReduceMotionEnabled) {
                    val left = size.width / widthFraction - size.width
                    val center = (sweepMillis() - lagMillis) * speed - halfBand - left
                    drawRect(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, highlight, Color.Transparent),
                            startX = center - halfBand,
                            endX = center + halfBand,
                        ),
                    )
                }
            },
    )
}

private val WelcomeRise = 3.dp
private const val WELCOME_APPEAR_MS = 300
private const val WELCOME_DELAY_MS = 300
private const val WELCOME_STAGGER_MS = 150
private const val WELCOME_EXIT_MS = 250
private const val WELCOME_EXIT_STAGGER_MS = 30
private val SkeletonWidthFractions = listOf(0.735f, 0.725f, 0.35f)
private val SkeletonHeight = 44.dp
private val SkeletonHalfBand = 110.dp
private const val SKELETON_SWEEP_DP_PER_MS = 0.56f
private const val SKELETON_SWEEP_PERIOD_MS = 1_400
private const val SKELETON_SWEEP_LAG_MS = 120
private const val SKELETON_FILL_ALPHA = 0.6f
private const val SKELETON_HIGHLIGHT_ALPHA = 0.18f
private const val SKELETON_APPEAR_MS = 330
private const val SKELETON_DELAY_MS = WELCOME_DELAY_MS + 350
private const val SKELETON_STAGGER_MS = 80
private const val STARTER_CROSS_FADE_MS = 250
private const val STARTER_CROSS_FADE_STAGGER_MS = 100
private const val SKELETON_EXIT_MS = STARTER_CROSS_FADE_MS + 2L * STARTER_CROSS_FADE_STAGGER_MS

@Composable
internal fun ChatFollowUps(
    followUps: List<String>,
    isSuggestionEnabled: Boolean,
    onClickSuggestion: (String, ChatInputType) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .bleedToEnd(FollowUpsEndBleed)
            .horizontalScroll(rememberScrollState())
            .padding(end = FollowUpsEndBleed),
    ) {
        followUps.forEach { followUp ->
            key(followUp) {
                ChatSuggestionChip(
                    text = followUp,
                    enabled = isSuggestionEnabled,
                    onClick = { onClickSuggestion(followUp, ChatInputType.FollowUp) },
                    theme = theme,
                )
            }
        }
    }
}

private fun Modifier.bleedToEnd(bleed: Dp) = layout { measurable, constraints ->
    val bleedPx = bleed.roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth + bleedPx))
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

private val FollowUpsEndBleed = 16.dp

@Composable
private fun ChatSuggestionChip(
    text: String,
    enabled: Boolean,
    onClick: (topInRoot: Float) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val position = remember { PlacedCoordinates() }
    Text(
        text = text,
        color = theme.primaryText,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        modifier = modifier
            .onPlaced(position::onPlaced)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .border(1.dp, theme.chipBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = { onClick(position.topInRoot()) })
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
