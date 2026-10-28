package au.com.shiftyjelly.pocketcasts.chat.ui

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
    onClickSuggestion: (String, ChatInputType) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val summarize = stringResource(LR.string.chat_suggestion_summarize)
    val animateStarterQuestions = remember { starterQuestions.isEmpty() }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier.fillMaxWidth(),
    ) {
        ChatAppear(
            animate = true,
            rise = WelcomeRise,
            durationMillis = WELCOME_APPEAR_MS,
            delayMillis = WELCOME_DELAY_MS,
        ) {
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
            ChatAppear(
                animate = true,
                rise = WelcomeRise,
                durationMillis = WELCOME_APPEAR_MS,
                delayMillis = WELCOME_DELAY_MS + WELCOME_STAGGER_MS,
            ) {
                ChatSuggestionChip(
                    text = summarize,
                    enabled = isSuggestionEnabled,
                    onClick = { onClickSuggestion(summarize, ChatInputType.SummaryPrompt) },
                    theme = theme,
                )
            }
            starterQuestions.forEach { question ->
                key(question) {
                    ChatAppear(animate = animateStarterQuestions) {
                        ChatSuggestionChip(
                            text = question,
                            enabled = isSuggestionEnabled,
                            onClick = { onClickSuggestion(question, ChatInputType.SuggestedPrompt) },
                            theme = theme,
                        )
                    }
                }
            }
        }
    }
}

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
private val WelcomeRise = 3.dp
private const val WELCOME_APPEAR_MS = 300
private const val WELCOME_DELAY_MS = 300
private const val WELCOME_STAGGER_MS = 150

@Composable
private fun ChatSuggestionChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = theme.primaryText,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .border(1.dp, theme.chipBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
