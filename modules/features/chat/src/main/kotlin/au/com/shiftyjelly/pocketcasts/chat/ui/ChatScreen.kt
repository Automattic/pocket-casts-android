package au.com.shiftyjelly.pocketcasts.chat.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
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
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatInputType
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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
    onClickSuggestion: (String, ChatInputType) -> Unit,
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
    val loadedMessageUuids = remember(uiState.episodeUuid, uiState.areMessagesLoaded) {
        uiState.messages.mapTo(HashSet()) { it.uuid }
    }
    var seenBeforeHistoryUuids by remember { mutableStateOf(emptySet<String>()) }
    var pendingQuestionUuid by remember { mutableStateOf<String?>(null) }
    var flyingSuggestion by remember { mutableStateOf<FlyingSuggestion?>(null) }
    LaunchedEffect(uiState.isHistoryVisible) {
        if (uiState.isHistoryVisible) {
            flyingSuggestion = flyingSuggestion?.copy(hasLanded = true)
            val pendingQuestion = uiState.messages.lastOrNull()?.takeIf { uiState.isAwaitingReply && it is ChatMessage.User }
            pendingQuestionUuid = pendingQuestion?.uuid
            seenBeforeHistoryUuids = seenBeforeHistoryUuids + uiState.messages.filter { it != pendingQuestion }.map { it.uuid }
        }
    }
    LaunchedEffect(pendingQuestionUuid, uiState.isAwaitingReply) {
        val uuid = pendingQuestionUuid
        if (uuid != null && !uiState.isAwaitingReply) {
            seenBeforeHistoryUuids = seenBeforeHistoryUuids + uuid
            pendingQuestionUuid = null
        }
    }
    LaunchedEffect(uiState.isAwaitingReply, flyingSuggestion?.hasLanded) {
        if (!uiState.isAwaitingReply && flyingSuggestion?.hasLanded == true) flyingSuggestion = null
    }
    var isWelcomeEntered by rememberSaveable(uiState.episodeUuid) { mutableStateOf(false) }
    LaunchedEffect(uiState.messages.isEmpty()) {
        if (uiState.messages.isNotEmpty()) isWelcomeEntered = false
    }
    val loadingPhase = rememberChatLoadingPhase(
        isAwaitingReply = uiState.isAwaitingReply,
        startDelay = flyingSuggestion?.remainingFlight() ?: Duration.ZERO,
    )
    val initialMessageUuids = remember(loadedMessageUuids, uiState.restoredMessageUuids, seenBeforeHistoryUuids) {
        loadedMessageUuids + uiState.restoredMessageUuids + seenBeforeHistoryUuids
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
                    useEpisodeArtwork = uiState.useEpisodeArtwork,
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
                        initialMessageUuids = initialMessageUuids,
                        loadingPhase = loadingPhase,
                        flyingSuggestion = flyingSuggestion,
                        onFlyingSuggestionChange = { flyingSuggestion = it },
                        isWelcomeEntered = isWelcomeEntered,
                        onWelcomeEnter = { isWelcomeEntered = true },
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
    initialMessageUuids: Set<String>,
    loadingPhase: ChatLoadingPhase,
    flyingSuggestion: FlyingSuggestion?,
    onFlyingSuggestionChange: (FlyingSuggestion?) -> Unit,
    isWelcomeEntered: Boolean,
    onWelcomeEnter: () -> Unit,
    theme: ChatTheme,
    onClickClose: () -> Unit,
    onClickMore: () -> Unit,
    onClickPlayPause: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClickSuggestion: (String, ChatInputType) -> Unit,
    onRetry: () -> Unit,
    onPlayQuote: (quoteUuid: String) -> Unit,
    onRateAnswer: (answerUuid: String, rating: ChatAnswerRating) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val isWelcomeVisible = uiState.areMessagesLoaded && uiState.messages.isEmpty() && !uiState.isAwaitingReply
    val currentOnWelcomeEnter by rememberUpdatedState(onWelcomeEnter)
    val welcomePosition = remember { PlacedCoordinates() }
    val contentPosition = remember { PlacedCoordinates() }
    var exitingWelcome by remember { mutableStateOf<ExitingWelcome?>(null) }
    fun exitWelcome(tappedSuggestion: String?) {
        if (isWelcomeVisible && !isReduceMotionEnabled) {
            exitingWelcome = ExitingWelcome(
                starterQuestions = uiState.starterQuestions,
                tappedSuggestion = tappedSuggestion,
                topInRoot = welcomePosition.topInRoot(),
            )
        }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val followThresholdPx = with(LocalDensity.current) { FollowNewestThreshold.roundToPx() }
    var isFollowingNewest by rememberSaveable { mutableStateOf(true) }
    var isUserScrolling by remember { mutableStateOf(false) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(scrollState) {
        scrollState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) isUserScrolling = true
        }
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .drop(1)
            .filter { inProgress -> !inProgress }
            .collect {
                if (isUserScrolling || !isAutoScrolling) {
                    isFollowingNewest = scrollState.maxValue - scrollState.value <= followThresholdPx
                }
                isUserScrolling = false
            }
    }
    val newestMessage = uiState.messages.lastOrNull()
    LaunchedEffect(newestMessage?.uuid, uiState.isAwaitingReply) {
        if (uiState.isAwaitingReply && newestMessage is ChatMessage.User) {
            isFollowingNewest = true
        }
    }
    val smoothFollowMaxPx by rememberUpdatedState(with(LocalDensity.current) { SmoothFollowMaxDistance.roundToPx() })
    val isSmoothFollowEnabled by rememberUpdatedState(!isReduceMotionEnabled)
    var hasFollowed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.messages.size, uiState.isAwaitingReply) {
        snapshotFlow { scrollState.maxValue }.collectLatest { maxValue ->
            if (isFollowingNewest && !isUserScrolling) {
                val distance = maxValue - scrollState.value
                val isSmooth = hasFollowed && distance in 1..smoothFollowMaxPx && isSmoothFollowEnabled
                if (maxValue > 0) hasFollowed = true
                isAutoScrolling = true
                try {
                    if (isSmooth) {
                        scrollState.animateScrollTo(
                            maxValue,
                            tween(durationMillis = SMOOTH_FOLLOW_MS, easing = EaseOut),
                        )
                    } else {
                        scrollState.scrollTo(maxValue)
                    }
                } finally {
                    isAutoScrolling = false
                }
            }
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

        Box(
            modifier = Modifier
                .weight(1f)
                .onPlaced(contentPosition::onPlaced),
        ) {
            exitingWelcome?.let { exiting ->
                key(exiting) {
                    ChatWelcome(
                        starterQuestions = exiting.starterQuestions,
                        isSuggestionEnabled = uiState.isConnected,
                        onClickSuggestion = { _, _, _ -> },
                        theme = theme,
                        exitingSuggestion = exiting.tappedSuggestion,
                        isExiting = true,
                        modifier = Modifier
                            .offset { IntOffset(0, (exiting.topInRoot - contentPosition.topInRoot()).roundToInt()) }
                            .padding(horizontal = 16.dp)
                            .clearAndSetSemantics {},
                    )
                    LaunchedEffect(Unit) {
                        delay(WELCOME_EXIT_TOTAL_MS)
                        exitingWelcome = null
                    }
                }
            }
            flyingSuggestion?.takeUnless { it.hasLanded }?.let { flight ->
                key(flight.startMillis) {
                    val contentTop = contentPosition.topInRoot()
                    ChatFlight(
                        fromY = flight.fromTopInRoot - contentTop,
                        toY = flight.toTopInRoot - contentTop,
                        onLand = { onFlyingSuggestionChange(flight.copy(hasLanded = true)) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clearAndSetSemantics {},
                    ) { progress ->
                        UserMessageBubble(
                            text = flight.text,
                            theme = theme,
                            chipMorph = { flightChipMorph(progress()) },
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            ) {
                val typingTransition = rememberInfiniteTransition(label = "typing")
                val awaitedQuestionUuid = uiState.messages.lastOrNull()?.uuid?.takeIf { uiState.isAwaitingReply }
                val enteredWhileAwaitingUuid = remember { awaitedQuestionUuid }
                val thinkingAlpha = remember { Animatable(if (awaitedQuestionUuid != null) 1f else 0f) }
                LaunchedEffect(awaitedQuestionUuid, isReduceMotionEnabled) {
                    when {
                        awaitedQuestionUuid == null -> Unit

                        isReduceMotionEnabled || awaitedQuestionUuid == enteredWhileAwaitingUuid -> thinkingAlpha.snapTo(1f)

                        else -> {
                            thinkingAlpha.snapTo(0f)
                            val settleMillis = flyingSuggestion?.remainingFlight()?.inWholeMilliseconds?.toInt() ?: 0
                            thinkingAlpha.animateTo(
                                1f,
                                tween(durationMillis = THINKING_APPEAR_MS, delayMillis = settleMillis + THINKING_DELAY_MS),
                            )
                        }
                    }
                }
                if (uiState.otherPlayingEpisode != null) {
                    ChatAboutEpisode(
                        episodeTitle = uiState.episodeTitle,
                        theme = theme,
                    )
                }
                if (isWelcomeVisible) {
                    val animateWelcome = remember { !isWelcomeEntered }
                    LaunchedEffect(Unit) { currentOnWelcomeEnter() }
                    val canSendSuggestion = !uiState.isAwaitingReply && uiState.isConnected && flyingSuggestion?.hasLanded != false
                    ChatWelcome(
                        starterQuestions = uiState.starterQuestions,
                        isSuggestionEnabled = uiState.isConnected,
                        onClickSuggestion = { text, inputType, topInRoot ->
                            if (canSendSuggestion && !isReduceMotionEnabled) {
                                onFlyingSuggestionChange(
                                    FlyingSuggestion(
                                        text = text,
                                        fromTopInRoot = topInRoot,
                                        toTopInRoot = welcomePosition.topInRoot(),
                                    ),
                                )
                                exitWelcome(tappedSuggestion = text)
                            }
                            onClickSuggestion(text, inputType)
                        },
                        theme = theme,
                        animate = animateWelcome,
                        modifier = Modifier
                            .onPlaced(welcomePosition::onPlaced)
                            .alpha(if (exitingWelcome != null) 0f else 1f),
                    )
                }
                val answerStream = rememberChatAnswerStream(uiState.messages, initialMessageUuids)
                val answerUuidsByLastIndex = uiState.answerUuidsByLastIndex
                uiState.messages.forEachIndexed { index, message ->
                    if (answerStream?.hasStarted(message.uuid) == false) return@forEachIndexed
                    key(message.uuid) {
                        val isNew = message.uuid !in initialMessageUuids
                        val replacesTypingIndicator = isNew && uiState.messages.getOrNull(index - 1) is ChatMessage.User
                        val isStreaming = answerStream?.isStreaming(message.uuid) == true
                        val skipStreamOnTap = if (isStreaming) {
                            Modifier.pointerInput(answerStream) {
                                detectTapGestures { coroutineScope.launch { answerStream.skip() } }
                            }
                        } else {
                            Modifier
                        }
                        when (message) {
                            is ChatMessage.Assistant -> ChatAnswerAppear(
                                animate = isNew,
                                replacesTypingIndicator = replacesTypingIndicator,
                                loadingPhase = loadingPhase,
                                typingTransition = typingTransition,
                                thinkingAlpha = { thinkingAlpha.value },
                                theme = theme,
                                modifier = skipStreamOnTap,
                            ) {
                                AiMessageBubble(
                                    text = message.displayText,
                                    theme = theme,
                                    revealProgress = answerStream?.takeIf { isStreaming }?.revealProgress(message.uuid),
                                )
                            }

                            is ChatMessage.Quote -> ChatAnswerAppear(
                                animate = isNew,
                                replacesTypingIndicator = replacesTypingIndicator,
                                loadingPhase = loadingPhase,
                                typingTransition = typingTransition,
                                thinkingAlpha = { thinkingAlpha.value },
                                theme = theme,
                                modifier = skipStreamOnTap,
                            ) {
                                AiQuoteBubble(
                                    quote = message.displayText,
                                    timestampLabel = message.timestampLabel,
                                    isPlayable = message.canPlay,
                                    isLoading = message.uuid == uiState.loadingQuoteUuid,
                                    theme = theme,
                                    revealProgress = answerStream?.takeIf { isStreaming }?.revealProgress(message.uuid),
                                    onClickPlay = { onPlayQuote(message.uuid) },
                                )
                            }

                            is ChatMessage.User -> {
                                val flewIn = remember { isNew && flyingSuggestion?.text == message.text }
                                val isInFlight = flewIn && flyingSuggestion?.hasLanded == false
                                val bubble = @Composable {
                                    UserMessageBubble(
                                        text = message.text,
                                        theme = theme,
                                        isNotSent = index == uiState.messages.lastIndex && uiState.error == ChatError.NetworkError,
                                        isRetryEnabled = uiState.isConnected,
                                        onRetry = onRetry,
                                        modifier = Modifier.alpha(if (isInFlight) 0f else 1f),
                                    )
                                }
                                if (flewIn) {
                                    bubble()
                                } else {
                                    ChatAppear(animate = isNew, rise = 8.dp, content = bubble)
                                }
                            }
                        }
                        answerUuidsByLastIndex[index]?.takeUnless { isStreaming }?.let { answerUuid ->
                            ChatAppear(
                                animate = isNew,
                                rise = AnswerFooterRise,
                                durationMillis = ANSWER_FOOTER_APPEAR_MS,
                                delayMillis = ANSWER_FOOTER_DELAY_MS,
                            ) {
                                ChatAnswerRatingRow(
                                    rating = uiState.answerRatings[answerUuid],
                                    onRate = { rating -> onRateAnswer(answerUuid, rating) },
                                    theme = theme,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                        if (index == uiState.messages.lastIndex && index in answerUuidsByLastIndex && !isStreaming) {
                            ChatAppear(
                                animate = isNew,
                                rise = AnswerFooterRise,
                                durationMillis = ANSWER_FOOTER_APPEAR_MS,
                                delayMillis = ANSWER_FOOTER_DELAY_MS,
                            ) {
                                ChatDisclaimer(theme = theme)
                            }
                            val followUps = uiState.followUps
                            if (followUps.isNotEmpty()) {
                                ChatAppear(
                                    animate = isNew,
                                    rise = FollowUpsRise,
                                    durationMillis = FOLLOW_UPS_APPEAR_MS,
                                    delayMillis = FOLLOW_UPS_DELAY_MS,
                                ) {
                                    ChatFollowUps(
                                        followUps = followUps,
                                        isSuggestionEnabled = uiState.isConnected,
                                        onClickSuggestion = onClickSuggestion,
                                        theme = theme,
                                    )
                                }
                            }
                        }
                    }
                }
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
                        phase = loadingPhase,
                        theme = theme,
                        transition = typingTransition,
                        modifier = Modifier.graphicsLayer { alpha = thinkingAlpha.value },
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
            onSend = {
                onFlyingSuggestionChange(null)
                exitWelcome(tappedSuggestion = null)
                onSend()
            },
            isConnected = uiState.isConnected,
            canSend = uiState.canSend,
            showBetaNote = uiState.isBetaNoteVisible,
            theme = theme,
        )
    }
}

private val FollowNewestThreshold = 48.dp
private val SmoothFollowMaxDistance = 400.dp
private const val SMOOTH_FOLLOW_MS = 250
private const val WELCOME_EXIT_TOTAL_MS = 500L
private const val THINKING_APPEAR_MS = 300
private const val THINKING_DELAY_MS = 250
private val AnswerFooterRise = 7.dp
private const val ANSWER_FOOTER_APPEAR_MS = 220
private const val ANSWER_FOOTER_DELAY_MS = 150
private val FollowUpsRise = 16.dp
private const val FOLLOW_UPS_APPEAR_MS = 300
private const val FOLLOW_UPS_DELAY_MS = 350

private data class FlyingSuggestion(
    val text: String,
    val fromTopInRoot: Float,
    val toTopInRoot: Float,
    val startMillis: Long = SystemClock.uptimeMillis(),
    val hasLanded: Boolean = false,
) {
    fun remainingFlight() = (FLY_IN_DURATION_MS - (SystemClock.uptimeMillis() - startMillis)).coerceAtLeast(0).milliseconds
}

private data class ExitingWelcome(
    val starterQuestions: List<String>,
    val tappedSuggestion: String?,
    val topInRoot: Float,
)

private fun ChatUiState.isAnswerLanding(initialMessageUuids: Set<String>): Boolean {
    val newestMessage = messages.lastOrNull()
    return error == null && newestMessage is ChatMessage.User && newestMessage.uuid !in initialMessageUuids
}

@Composable
private fun ChatAnswerAppear(
    animate: Boolean,
    replacesTypingIndicator: Boolean,
    loadingPhase: ChatLoadingPhase,
    typingTransition: InfiniteTransition,
    thinkingAlpha: () -> Float,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        ChatAppear(animate = animate, durationMillis = ANSWER_CROSS_FADE_MS, content = content)
        if (replacesTypingIndicator) {
            ChatFadeOut(durationMillis = ANSWER_CROSS_FADE_MS) {
                ThinkingBubble(
                    phase = loadingPhase,
                    theme = theme,
                    transition = typingTransition,
                    modifier = Modifier.graphicsLayer { alpha = thinkingAlpha() },
                )
            }
        }
    }
}

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
                    isBetaNoteVisible = true,
                    areMessagesLoaded = true,
                    messages = listOf(
                        ChatMessage.User(
                            text = "What was the main point?",
                        ),
                        ChatMessage.Assistant(
                            text = "The hosts focused on making discovery feel personal without adding friction.",
                            followUps = listOf(
                                "How do recommendations work?",
                                "Who is the guest?",
                                "What changed this year?",
                            ),
                        ),
                        ChatMessage.Quote(
                            text = "The important idea is to keep the interface simple while still surfacing useful context.",
                            start = "12:04",
                            end = "12:18",
                            canPlay = true,
                        ),
                        ChatMessage.Quote(
                            text = "Discovery works best when it feels like a friend's recommendation.",
                            start = "24:30",
                            end = "24:41",
                            canPlay = true,
                            uuid = "preview-loading-quote",
                        ),
                    ),
                    loadingQuoteUuid = "preview-loading-quote",
                ),
                onClickClose = {},
                onClickMore = {},
                onCloseHistory = {},
                onOpenHistoryChat = {},
                onDeleteHistoryChat = {},
                onClickPlayPause = {},
                onInputTextChange = {},
                onSend = {},
                onClickSuggestion = { _, _ -> },
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
                    isBetaNoteVisible = true,
                    areMessagesLoaded = true,
                    starterQuestions = listOf(
                        "Who is the guest?",
                        "How do recommendations work?",
                        "What changed this year?",
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
                onClickSuggestion = { _, _ -> },
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
                    isBetaNoteVisible = true,
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
                onClickSuggestion = { _, _ -> },
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
