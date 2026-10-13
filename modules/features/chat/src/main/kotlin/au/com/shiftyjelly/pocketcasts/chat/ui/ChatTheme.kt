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
    val aiBubble: Color,
    val aiBubbleText: Color,
    val userBubble: Color,
    val userBubbleText: Color,
    val inputBackground: Color,
    val inputText: Color,
    val inputHint: Color,
    val sendButton: Color,
    val sendButtonIcon: Color,
    val divider: Color,
    val closeButtonBackground: Color,
    val progress: Color,
    val progressTrack: Color,
    val chipBorder: Color,
    val quoteTimestamp: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = ChatTheme(
            background = colors.primaryUi01,
            primaryText = colors.primaryText01,
            secondaryText = colors.primaryText02,
            iconButton = colors.primaryIcon01,
            aiBubble = colors.primaryUi05,
            aiBubbleText = colors.primaryText01,
            userBubble = colors.primaryInteractive01,
            userBubbleText = colors.primaryInteractive02,
            inputBackground = colors.primaryField01,
            inputText = colors.primaryText01,
            inputHint = colors.primaryText02,
            sendButton = colors.primaryInteractive01,
            sendButtonIcon = colors.primaryInteractive02,
            divider = colors.primaryUi05,
            closeButtonBackground = colors.primaryUi05,
            progress = colors.primaryInteractive01,
            progressTrack = colors.primaryUi05,
            chipBorder = colors.primaryUi05,
            quoteTimestamp = colors.primaryInteractive01,
        )

        fun player(colors: PlayerColors) = ChatTheme(
            background = colors.background01,
            primaryText = colors.contrast01,
            secondaryText = colors.contrast04,
            iconButton = colors.contrast01,
            aiBubble = colors.contrast05,
            aiBubbleText = colors.contrast01,
            userBubble = colors.contrast06,
            userBubbleText = colors.contrast01,
            inputBackground = colors.contrast06,
            inputText = colors.contrast01,
            inputHint = colors.contrast03,
            sendButton = colors.contrast01,
            sendButtonIcon = colors.background01,
            divider = colors.contrast05,
            closeButtonBackground = colors.contrast05,
            progress = colors.highlight01,
            progressTrack = colors.contrast05,
            chipBorder = colors.contrast05,
            quoteTimestamp = QuoteTimestampBlue,
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

private val QuoteTimestampBlue = Color(0xFF2A67F9)
