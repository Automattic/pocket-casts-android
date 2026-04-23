package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ChatSuggestionChips(
    suggestions: List<String>,
    onClickSuggestion: (String) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                text = suggestion,
                onClick = { onClickSuggestion(suggestion) },
                theme = theme,
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = theme.suggestionText,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = modifier
            .clip(ChipShape)
            .border(1.dp, theme.suggestionBorder, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

private val ChipShape = RoundedCornerShape(20.dp)
