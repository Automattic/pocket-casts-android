package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.chat.ChatError
import au.com.shiftyjelly.pocketcasts.chat.ChatUiState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onPlayQuote: (quoteUuid: String) -> Unit,
    onDismissBetaSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = rememberChatTheme()
    val betaSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val currentOnDismissBetaSheet by rememberUpdatedState(onDismissBetaSheet)

    LaunchedEffect(uiState.isBetaSheetVisible) {
        if (uiState.isBetaSheetVisible) betaSheetState.show() else betaSheetState.hide()
    }
    LaunchedEffect(betaSheetState) {
        snapshotFlow { betaSheetState.currentValue }
            .drop(1)
            .filter { it == ModalBottomSheetValue.Hidden }
            .collect { currentOnDismissBetaSheet() }
    }

    ModalBottomSheetLayout(
        sheetState = betaSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        sheetContent = {
            ChatBetaSheet(
                theme = theme,
                onClickGotIt = onDismissBetaSheet,
            )
        },
        modifier = modifier,
    ) {
        ChatContent(
            uiState = uiState,
            theme = theme,
            onClickClose = onClickClose,
            onClickMore = onClickMore,
            onInputTextChange = onInputTextChange,
            onSend = onSend,
            onRetry = onRetry,
            onPlayQuote = onPlayQuote,
        )
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    theme: ChatTheme,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onPlayQuote: (quoteUuid: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
            podcastTitle = uiState.podcastTitle,
            onClickBack = onClickClose,
            onClickMore = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                onClickMore()
            },
            theme = theme,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ChatContextBubble(
                episodeDurationMs = uiState.episodeDurationMs,
                theme = theme,
            )
            AiMessageBubble(
                text = stringResource(LR.string.chat_preview_ai_1),
                podcastUuid = uiState.podcastUuid,
                theme = theme,
            )
            uiState.messages.forEachIndexed { index, message ->
                when (message) {
                    is ChatMessage.Assistant -> AiMessageBubble(
                        text = message.displayText,
                        podcastUuid = uiState.podcastUuid,
                        theme = theme,
                    )

                    is ChatMessage.Quote -> AiQuoteBubble(
                        quote = message.displayText,
                        timestampLabel = message.timestampLabel,
                        isPlayable = message.canPlay,
                        isPlaying = message.isPlaying,
                        theme = theme,
                        onClickPlay = { onPlayQuote(message.uuid) },
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
                ThinkingBubble(theme = theme)
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
            showBetaNote = uiState.isBeta,
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

@Preview
@Composable
private fun ChatScreenPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ChatScreen(
            uiState = ChatUiState(
                inputText = "Ask a follow-up",
                episodeTitle = "The future of podcast discovery",
                episodeSubtitle = "May 25",
                podcastUuid = "preview-podcast-uuid",
                podcastTitle = "Pocket Casts Weekly",
                episodeDurationMs = 3_600_000,
                isBeta = true,
                messages = listOf(
                    ChatMessage.User(
                        text = "What was the main point?",
                    ),
                    ChatMessage.Assistant(
                        text = "The hosts focused on making discovery feel personal without adding friction.",
                    ),
                    ChatMessage.Quote(
                        text = "The important idea is to keep the interface simple while still surfacing useful context.",
                        start = "12:04",
                        end = "12:18",
                        canPlay = true,
                    ),
                ),
            ),
            onClickClose = {},
            onClickMore = {},
            onInputTextChange = {},
            onSend = {},
            onRetry = {},
            onPlayQuote = {},
            onDismissBetaSheet = {},
        )
    }
}
