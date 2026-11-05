package au.com.shiftyjelly.pocketcasts.chat.ui

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatInputType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatWelcome(
    starterQuestions: List<String>,
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
    val initialStarterCount = remember { starterQuestions.size }
    val welcomeStartMillis = remember { SystemClock.uptimeMillis() }
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
            starterQuestions.forEachIndexed { index, question ->
                key(question) {
                    val isLate = index >= initialStarterCount
                    val delayMillis = remember {
                        val elapsedMillis = (SystemClock.uptimeMillis() - welcomeStartMillis).toInt()
                        val entranceMillis = if (animate) STARTER_DELAY_MS + index * STARTER_STAGGER_MS - elapsedMillis else 0
                        maxOf(entranceMillis, (index - initialStarterCount).coerceAtLeast(0) * STARTER_STAGGER_MS)
                    }
                    WelcomeItem(
                        order = index + 2,
                        isExiting = isExiting,
                        animate = animate || isLate,
                        delayMillis = delayMillis,
                        isHeld = isTapped(question),
                    ) {
                        ChatSuggestionChip(
                            text = question,
                            enabled = isSuggestionEnabled,
                            onClick = { topInRoot -> onClickSuggestion(question, ChatInputType.SuggestedPrompt, topInRoot) },
                            theme = theme,
                            modifier = Modifier.hiddenIf(isTapped(question)),
                        )
                    }
                }
            }
        }
    }
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

private val WelcomeRise = 3.dp
private const val WELCOME_APPEAR_MS = 300
private const val WELCOME_DELAY_MS = 300
private const val WELCOME_STAGGER_MS = 150
private const val WELCOME_EXIT_MS = 250
private const val WELCOME_EXIT_STAGGER_MS = 30
private const val STARTER_DELAY_MS = WELCOME_DELAY_MS + 350
private const val STARTER_STAGGER_MS = 80

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
