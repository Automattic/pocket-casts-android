package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.chat.ChatRole
import au.com.shiftyjelly.pocketcasts.chat.ChatUiState

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = rememberChatTheme()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.messages.size, scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .imePadding(),
    ) {
        ChatToolbar(
            episodeTitle = uiState.episodeTitle,
            episodeSubtitle = uiState.episodeSubtitle,
            podcastUuid = uiState.podcastUuid,
            onClickBack = onClickClose,
            onClickMore = onClickMore,
            theme = theme,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            uiState.messages.forEach { message ->
                when (message.role) {
                    ChatRole.Ai -> AiMessageBubble(text = message.text, podcastUuid = uiState.podcastUuid, theme = theme)
                    ChatRole.User -> UserMessageBubble(text = message.text, theme = theme)
                }
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            onTextChange = onInputTextChange,
            onSend = onSend,
            isConnected = uiState.isConnected,
            canSend = uiState.canSend,
            theme = theme,
        )
    }
}
