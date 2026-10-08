package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import au.com.shiftyjelly.pocketcasts.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.chat.ChatError
import au.com.shiftyjelly.pocketcasts.chat.ChatUiState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onClickPlayPause: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClickSuggestion: (String) -> Unit,
    onRetry: () -> Unit,
    onPlayQuote: (quoteUuid: String) -> Unit,
    onRateAnswer: (answerUuid: String, rating: ChatAnswerRating) -> Unit,
    onDismissBetaSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = rememberChatTheme()
    val currentOnDismissBetaSheet by rememberUpdatedState(onDismissBetaSheet)
    val betaSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
        confirmValueChange = { value ->
            if (value == ModalBottomSheetValue.Hidden) currentOnDismissBetaSheet()
            true
        },
    )

    LaunchedEffect(uiState.isBetaSheetVisible) {
        if (uiState.isBetaSheetVisible) betaSheetState.show() else betaSheetState.hide()
    }
    BackHandler(enabled = uiState.isBetaSheetVisible) {
        onDismissBetaSheet()
    }

    ModalBottomSheetLayout(
        sheetState = betaSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = theme.background,
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
            onClickPlayPause = onClickPlayPause,
            onInputTextChange = onInputTextChange,
            onSend = onSend,
            onClickSuggestion = onClickSuggestion,
            onRetry = onRetry,
            onPlayQuote = onPlayQuote,
            onRateAnswer = onRateAnswer,
        )
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    theme: ChatTheme,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onClickPlayPause: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClickSuggestion: (String) -> Unit,
    onRetry: () -> Unit,
    onPlayQuote: (quoteUuid: String) -> Unit,
    onRateAnswer: (answerUuid: String, rating: ChatAnswerRating) -> Unit,
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
        ChatHeader(
            episodeTitle = uiState.episodeTitle,
            podcastUuid = uiState.podcastUuid,
            podcastTitle = uiState.podcastTitle,
            playback = uiState.playback,
            onClickClose = onClickClose,
            onClickPlayPause = onClickPlayPause,
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
            if (uiState.areMessagesLoaded) {
                ChatWelcome(
                    isConversationStarted = uiState.messages.isNotEmpty() || uiState.isAwaitingReply,
                    isConnected = uiState.isConnected,
                    onClickSuggestion = onClickSuggestion,
                    theme = theme,
                )
            }
            val answerUuidsByLastIndex = uiState.answerUuidsByLastIndex
            uiState.messages.forEachIndexed { index, message ->
                when (message) {
                    is ChatMessage.Assistant -> AiMessageBubble(
                        text = message.displayText,
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
                answerUuidsByLastIndex[index]?.let { answerUuid ->
                    ChatAnswerRatingRow(
                        rating = uiState.answerRatings[answerUuid],
                        onRate = { rating -> onRateAnswer(answerUuid, rating) },
                        theme = theme,
                    )
                }
            }
            if (uiState.messages.lastOrNull().isAnswer && !uiState.isAwaitingReply) {
                ChatDisclaimer(theme = theme)
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

private val ChatMessage?.isAnswer get() = this is ChatMessage.Assistant || this is ChatMessage.Quote

@Composable
private fun ChatDisclaimer(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(LR.string.chat_ai_disclaimer),
        color = theme.secondaryText,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = modifier.fillMaxWidth(),
    )
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
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppThemeWithBackground(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            ChatScreen(
                uiState = ChatUiState(
                    inputText = "Ask a follow-up",
                    episodeTitle = "The future of podcast discovery",
                    podcastUuid = "preview-podcast-uuid",
                    podcastTitle = "Pocket Casts Weekly",
                    episodeDurationMs = 3_600_000,
                    isBeta = true,
                    areMessagesLoaded = true,
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
                onClickPlayPause = {},
                onInputTextChange = {},
                onSend = {},
                onClickSuggestion = {},
                onRetry = {},
                onPlayQuote = {},
                onRateAnswer = { _, _ -> },
                onDismissBetaSheet = {},
            )
        }
    }
}

@Preview
@Composable
private fun ChatScreenEmptyPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppThemeWithBackground(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            ChatScreen(
                uiState = ChatUiState(
                    episodeTitle = "The future of podcast discovery",
                    podcastUuid = "preview-podcast-uuid",
                    podcastTitle = "Pocket Casts Weekly",
                    isBeta = true,
                    areMessagesLoaded = true,
                ),
                onClickClose = {},
                onClickMore = {},
                onClickPlayPause = {},
                onInputTextChange = {},
                onSend = {},
                onClickSuggestion = {},
                onRetry = {},
                onPlayQuote = {},
                onRateAnswer = { _, _ -> },
                onDismissBetaSheet = {},
            )
        }
    }
}
