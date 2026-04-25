package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.chat.ChatError
import au.com.shiftyjelly.pocketcasts.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.chat.ChatUiState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = rememberChatTheme()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.messages.size, uiState.isAwaitingReply, scrollState.maxValue) {
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
            uiState.messages.forEachIndexed { index, message ->
                when (message) {
                    is ChatMessage.Assistant -> AiMessageBubble(
                        text = message.text,
                        podcastUuid = uiState.podcastUuid,
                        theme = theme,
                    )
                    is ChatMessage.Quote -> AiQuoteBubble(
                        quote = message.text,
                        start = message.start,
                        end = message.end,
                        theme = theme,
                    )
                    is ChatMessage.User -> UserMessageBubble(
                        text = message.text,
                        theme = theme,
                        allowRetry = index == uiState.messages.lastIndex && uiState.error != null,
                        onRetry = onRetry,
                    )
                }
            }
            if (uiState.isAwaitingReply) {
                ThinkingBubble(podcastUuid = uiState.podcastUuid, theme = theme)
            }
            if (uiState.error != null) {
                ChatErrorMessage(error = uiState.error, theme = theme)
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

@Composable
private fun ChatErrorMessage(
    error: ChatError,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    val messageRes = when (error) {
        ChatError.ServerError -> LR.string.chat_error_server
        ChatError.NetworkError -> LR.string.chat_error_network
    }
    Text(
        text = stringResource(messageRes),
        color = theme.aiBubbleText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    )
}
