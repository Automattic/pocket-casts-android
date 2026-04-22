package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.theme

internal data class ChatTheme(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val iconButton: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = ChatTheme(
            background = colors.primaryUi01,
            primaryText = colors.primaryText01,
            secondaryText = colors.primaryText02,
            iconButton = colors.primaryIcon01,
        )

        fun player(colors: PlayerColors) = ChatTheme(
            background = colors.background01,
            primaryText = colors.contrast02,
            secondaryText = colors.contrast04,
            iconButton = colors.contrast02,
        )
    }
}

@Composable
internal fun rememberChatTheme(): ChatTheme {
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()

    return remember(theme.type, playerColors) {
        if (playerColors != null) {
            ChatTheme.player(playerColors)
        } else {
            ChatTheme.default(theme.colors)
        }
    }
}
