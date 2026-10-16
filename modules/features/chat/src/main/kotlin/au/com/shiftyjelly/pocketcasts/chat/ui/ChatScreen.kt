package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.chat.ChatError
import au.com.shiftyjelly.pocketcasts.chat.ChatUiState
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onCloseHistory: () -> Unit,
    onOpenHistoryChat: (EpisodeChatHistoryItem) -> Unit,
    onDeleteHistoryChat: (EpisodeChatHistoryItem) -> Unit,
    onClickPlayPause: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClickSuggestion: (String) -> Unit,
    onRetry: () -> Unit,
    onPlayQuote: (quoteUuid: String) -> Unit,
    onRateAnswer: (answerUuid: String, rating: ChatAnswerRating) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDismissBetaSheet: () -> Unit,
    onSignInClick: () -> Unit,
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

    ProvideTextStyle(LocalTextStyle.current.copy(letterSpacing = 0.sp)) {
        ModalBottomSheetLayout(
            sheetState = betaSheetState,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetBackgroundColor = theme.background,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            sheetContent = {
                ChatBetaSheet(
                    theme = theme,
                    buttonText = stringResource(if (uiState.isSignInRequired) LR.string.chat_beta_sheet_sign_in else LR.string.got_it),
                    onClickButton = if (uiState.isSignInRequired) onSignInClick else onDismissBetaSheet,
                )
            },
            modifier = modifier,
        ) {
            if (uiState.isHistoryVisible) {
                ChatHistory(
                    sections = uiState.history,
                    onClickBack = onCloseHistory,
                    onClickChat = onOpenHistoryChat,
                    onDeleteChat = onDeleteHistoryChat,
                    snackbarHostState = snackbarHostState,
                    theme = theme,
                )
            } else {
                ProvideReduceMotion {
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
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
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
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val loadedMessageUuids = remember(uiState.areMessagesLoaded) { uiState.messages.mapTo(HashSet()) { it.uuid } }
    val initialMessageUuids = remember(loadedMessageUuids, uiState.restoredMessageUuids) {
        loadedMessageUuids + uiState.restoredMessageUuids
    }

    val followThresholdPx = with(LocalDensity.current) { FollowNewestThreshold.roundToPx() }
    var isFollowingNewest by remember { mutableStateOf(true) }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .filter { inProgress -> !inProgress }
            .collect { isFollowingNewest = scrollState.maxValue - scrollState.value <= followThresholdPx }
    }
    val newestMessage = uiState.messages.lastOrNull()
    LaunchedEffect(newestMessage?.uuid, uiState.isAwaitingReply) {
        if (uiState.isAwaitingReply && newestMessage is ChatMessage.User) {
            isFollowingNewest = true
        }
    }
    LaunchedEffect(uiState.messages.size, uiState.isAwaitingReply, scrollState.maxValue) {
        if (isFollowingNewest) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .imePadding(),
    ) {
        val playingEpisode = uiState.otherPlayingEpisode
        ChatHeader(
            episodeTitle = playingEpisode?.episodeTitle ?: uiState.episodeTitle,
            podcastUuid = playingEpisode?.podcastUuid ?: uiState.podcastUuid,
            podcastTitle = playingEpisode?.podcastTitle ?: uiState.podcastTitle,
            userEpisode = playingEpisode?.userEpisode,
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

        Box(modifier = Modifier.weight(1f)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            ) {
                val typingTransition = rememberInfiniteTransition(label = "typing")
                if (uiState.otherPlayingEpisode != null) {
                    ChatAboutEpisode(
                        episodeTitle = uiState.episodeTitle,
                        theme = theme,
                    )
                }
                if (uiState.areMessagesLoaded && uiState.messages.isEmpty() && !uiState.isAwaitingReply) {
                    ChatWelcome(
                        isSuggestionEnabled = uiState.isConnected,
                        onClickSuggestion = onClickSuggestion,
                        theme = theme,
                    )
                }
                val answerUuidsByLastIndex = uiState.answerUuidsByLastIndex
                val latestAnswerLastIndex = answerUuidsByLastIndex.keys.maxOrNull()
                uiState.messages.forEachIndexed { index, message ->
                    key(message.uuid) {
                        val isNew = message.uuid !in initialMessageUuids
                        val replacesTypingIndicator = isNew && uiState.messages.getOrNull(index - 1) is ChatMessage.User
                        when (message) {
                            is ChatMessage.Assistant -> ChatAnswerAppear(
                                animate = isNew,
                                replacesTypingIndicator = replacesTypingIndicator,
                                typingTransition = typingTransition,
                                theme = theme,
                            ) {
                                AiMessageBubble(
                                    text = message.displayText,
                                    theme = theme,
                                )
                            }

                            is ChatMessage.Quote -> ChatAnswerAppear(
                                animate = isNew,
                                replacesTypingIndicator = replacesTypingIndicator,
                                typingTransition = typingTransition,
                                theme = theme,
                            ) {
                                AiQuoteBubble(
                                    quote = message.displayText,
                                    timestampLabel = message.timestampLabel,
                                    isPlayable = message.canPlay,
                                    theme = theme,
                                    onClickPlay = { onPlayQuote(message.uuid) },
                                )
                            }

                            is ChatMessage.User -> ChatAppear(animate = isNew, rise = 8.dp) {
                                UserMessageBubble(
                                    text = message.text,
                                    theme = theme,
                                    isNotSent = index == uiState.messages.lastIndex && uiState.error == ChatError.NetworkError,
                                    isRetryEnabled = uiState.isConnected,
                                    onRetry = onRetry,
                                )
                            }
                        }
                        answerUuidsByLastIndex[index]?.let { answerUuid ->
                            ChatAppear(animate = isNew) {
                                ChatAnswerRatingRow(
                                    rating = uiState.answerRatings[answerUuid],
                                    onRate = { rating -> onRateAnswer(answerUuid, rating) },
                                    theme = theme,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                        if (index == latestAnswerLastIndex) {
                            ChatAppear(animate = isNew) {
                                ChatDisclaimer(theme = theme)
                            }
                        }
                    }
                }
                val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
                AnimatedVisibility(
                    visible = uiState.isAwaitingReply || uiState.isAnswerLanding(initialMessageUuids),
                    enter = EnterTransition.None,
                    exit = if (uiState.error != null && !isReduceMotionEnabled) {
                        fadeOut(tween(ANSWER_CROSS_FADE_MS))
                    } else {
                        ExitTransition.None
                    },
                ) {
                    ThinkingBubble(
                        theme = theme,
                        modifier = Modifier.padding(top = TypingIndicatorExtraGap),
                        transition = typingTransition,
                    )
                }
                if (uiState.error == ChatError.ServerError) {
                    ChatServerError(theme = theme, isRetryEnabled = uiState.isConnected, onRetry = onRetry)
                }
            }
            ChatAnsweringAnnouncement(
                isAnswering = uiState.isAwaitingReply,
                answer = uiState.messages.newestAnswerText(excluding = initialMessageUuids),
            )
            ChatSnackbarHost(
                hostState = snackbarHostState,
                theme = theme,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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

private val FollowNewestThreshold = 48.dp

private fun ChatUiState.isAnswerLanding(initialMessageUuids: Set<String>): Boolean {
    val newestMessage = messages.lastOrNull()
    return error == null && newestMessage is ChatMessage.User && newestMessage.uuid !in initialMessageUuids
}
private val TypingIndicatorExtraGap = 8.dp

@Composable
private fun ChatAnswerAppear(
    animate: Boolean,
    replacesTypingIndicator: Boolean,
    typingTransition: InfiniteTransition,
    theme: ChatTheme,
    content: @Composable () -> Unit,
) {
    Box {
        ChatAppear(animate = animate, durationMillis = ANSWER_CROSS_FADE_MS, content = content)
        if (replacesTypingIndicator) {
            ChatFadeOut(durationMillis = ANSWER_CROSS_FADE_MS) {
                ThinkingBubble(
                    theme = theme,
                    modifier = Modifier.padding(top = TypingIndicatorExtraGap),
                    transition = typingTransition,
                )
            }
        }
    }
}

private val ChatMessage?.isAnswer get() = this is ChatMessage.Assistant || this is ChatMessage.Quote

@Composable
private fun ChatAboutEpisode(
    episodeTitle: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(LR.string.chat_about_episode, episodeTitle),
        color = theme.inputHint,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ChatDisclaimer(
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(LR.string.chat_ai_disclaimer),
        color = theme.inputHint,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ChatAnsweringAnnouncement(
    isAnswering: Boolean,
    answer: String?,
    modifier: Modifier = Modifier,
) {
    val answering = stringResource(LR.string.chat_answering)
    val target = when {
        isAnswering -> answering
        else -> answer
    }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        announcement = target
        delay(ANNOUNCEMENT_CLEAR_DELAY_MS)
        announcement = ""
    }
    Box(
        modifier = modifier
            .size(1.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            },
    )
}

private const val ANNOUNCEMENT_CLEAR_DELAY_MS = 3_000L

private fun List<ChatMessage>.newestAnswerText(excluding: Set<String>): String? {
    val answer = takeLastWhile { it.isAnswer }
    if (answer.isEmpty() || answer.any { it.uuid in excluding }) return null
    return answer.joinToString(separator = "\n") { message ->
        when (message) {
            is ChatMessage.Assistant -> message.displayText
            is ChatMessage.Quote -> message.displayText
            is ChatMessage.User -> message.text
        }
    }
}

@Composable
private fun ChatServerError(
    theme: ChatTheme,
    isRetryEnabled: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_alert_small),
                contentDescription = null,
                tint = theme.tertiaryText,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(LR.string.chat_error_server_on_our_end),
                color = theme.primaryText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .border(1.dp, theme.chipBorder, CircleShape)
                .clickable(enabled = isRetryEnabled, role = Role.Button, onClick = onRetry)
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_retry),
                contentDescription = null,
                tint = theme.primaryText,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(LR.string.try_again),
                color = theme.primaryText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
    }
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
                onCloseHistory = {},
                onOpenHistoryChat = {},
                onDeleteHistoryChat = {},
                onClickPlayPause = {},
                onInputTextChange = {},
                onSend = {},
                onClickSuggestion = {},
                onRetry = {},
                onPlayQuote = {},
                onRateAnswer = { _, _ -> },
                snackbarHostState = remember { SnackbarHostState() },
                onDismissBetaSheet = {},
                onSignInClick = {},
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
                onCloseHistory = {},
                onOpenHistoryChat = {},
                onDeleteHistoryChat = {},
                onClickPlayPause = {},
                onInputTextChange = {},
                onSend = {},
                onClickSuggestion = {},
                onRetry = {},
                onPlayQuote = {},
                onRateAnswer = { _, _ -> },
                snackbarHostState = remember { SnackbarHostState() },
                onDismissBetaSheet = {},
                onSignInClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun ChatScreenErrorPreview(
    @PreviewParameter(ChatErrorPreviewParameterProvider::class) error: ChatError,
) {
    AppThemeWithBackground(ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides PodcastColors.ForUserEpisode) {
            ChatScreen(
                uiState = ChatUiState(
                    episodeTitle = "The future of podcast discovery",
                    podcastUuid = "preview-podcast-uuid",
                    podcastTitle = "Pocket Casts Weekly",
                    isBeta = true,
                    areMessagesLoaded = true,
                    error = error,
                    messages = listOf(
                        ChatMessage.User(
                            text = "Any remarkable quotes from the episode?",
                        ),
                    ),
                ),
                onClickClose = {},
                onClickMore = {},
                onCloseHistory = {},
                onOpenHistoryChat = {},
                onDeleteHistoryChat = {},
                onClickPlayPause = {},
                onInputTextChange = {},
                onSend = {},
                onClickSuggestion = {},
                onRetry = {},
                onPlayQuote = {},
                onRateAnswer = { _, _ -> },
                snackbarHostState = remember { SnackbarHostState() },
                onDismissBetaSheet = {},
                onSignInClick = {},
            )
        }
    }
}

private class ChatErrorPreviewParameterProvider : PreviewParameterProvider<ChatError> {
    override val values = sequenceOf(ChatError.NetworkError, ChatError.ServerError)
}
