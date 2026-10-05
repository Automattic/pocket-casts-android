package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatWelcome(
    isConversationStarted: Boolean,
    isConnected: Boolean,
    onClickSuggestion: (String) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val summarize = stringResource(LR.string.chat_suggestion_summarize)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = if (isConversationStarted) Alignment.Start else Alignment.End,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(LR.string.chat_welcome),
            color = theme.primaryText,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        ChatSuggestionChip(
            text = summarize,
            enabled = !isConversationStarted && isConnected,
            onClick = { onClickSuggestion(summarize) },
            theme = theme,
        )
    }
}

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
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .border(1.dp, theme.chipBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
