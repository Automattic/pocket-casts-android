package au.com.shiftyjelly.pocketcasts.chat

import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatScreen
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ChatFragment : BaseDialogFragment() {
    override val statusBarIconColor = StatusBarIconColor.Light

    companion object {
        private const val ARGS_KEY = "chat_args"

        fun newInstance(
            episodeUuid: String,
            podcastUuid: String,
            podcastTitle: String,
            episodeTitle: String,
            episodeDurationMs: Int,
            sourceView: SourceView,
            isBeta: Boolean,
            podcastColors: PodcastColors,
        ) = ChatFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    ARGS_KEY,
                    Args(
                        episodeUuid,
                        podcastUuid,
                        podcastTitle,
                        episodeTitle,
                        episodeDurationMs,
                        sourceView,
                        isBeta,
                        podcastColors,
                    ),
                )
            }
        }
    }

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private val viewModel by viewModels<ChatViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setEpisodeInfo(
            episodeUuid = args.episodeUuid,
            episodeTitle = args.episodeTitle,
            podcastUuid = args.podcastUuid,
            podcastTitle = args.podcastTitle,
            episodeDurationMs = args.episodeDurationMs,
            sourceView = args.sourceView,
            isBeta = args.isBeta,
        )
        childFragmentManager.setFragmentResultListener(ChatFeedbackFragment.SUBMITTED_RESULT_KEY, this) { _, _ ->
            viewModel.onFeedbackSubmitted()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val resources = LocalResources.current
        var shownToast by remember { mutableStateOf<ChatToast?>(null) }
        LaunchedEffect(snackbarHostState) {
            viewModel.toasts.collectLatest { toast ->
                snackbarHostState.currentSnackbarData?.dismiss()
                shownToast = toast
                val result = snackbarHostState.showSnackbar(
                    message = toast.message(resources),
                    actionLabel = resources.getString(LR.string.chat_undo).takeIf { toast.isUndoable },
                    duration = if (toast.isUndoable) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                shownToast = null
                if (result == SnackbarResult.ActionPerformed) {
                    when (toast) {
                        ChatToast.ChatCleared -> viewModel.undoClearChat()
                        ChatToast.ChatDeleted -> viewModel.undoDeleteChat()
                        is ChatToast.QuotePlaying -> viewModel.undoQuotePlay()
                        ChatToast.FeedbackThanks -> Unit
                    }
                } else if (toast is ChatToast.QuotePlaying) {
                    viewModel.onQuoteUndoExpired()
                }
            }
        }
        val hasMessages = uiState.messages.isNotEmpty()
        LaunchedEffect(hasMessages) {
            if (hasMessages && shownToast == ChatToast.ChatCleared) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }

        AppTheme(theme.activeTheme) {
            CompositionLocalProvider(LocalPodcastColors provides uiState.podcastColors) {
                val backgroundColor = MaterialTheme.theme.rememberPlayerColorsOrDefault().background01
                LaunchedEffect(backgroundColor) {
                    setDialogTint(backgroundColor.toArgb())
                }

                ChatScreen(
                    uiState = uiState,
                    onClickClose = { dismiss() },
                    onClickMore = ::showOptionsDialog,
                    onCloseHistory = viewModel::closeHistory,
                    onOpenHistoryChat = viewModel::openHistoryChat,
                    onDeleteHistoryChat = viewModel::deleteHistoryChat,
                    onClickPlayPause = viewModel::onPlayPauseClick,
                    onInputTextChange = viewModel::onInputTextChange,
                    onSend = viewModel::onSend,
                    onClickSuggestion = viewModel::onSummarizeClick,
                    onRetry = viewModel::retry,
                    onPlayQuote = viewModel::playQuote,
                    onRateAnswer = ::rateAnswer,
                    snackbarHostState = snackbarHostState,
                    onDismissBetaSheet = viewModel::dismissBetaSheet,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun rateAnswer(answerUuid: String, rating: ChatAnswerRating) {
        if (viewModel.rateAnswer(answerUuid, rating) != ChatAnswerRating.Negative) return
        val uiState = viewModel.uiState.value
        ChatFeedbackFragment.show(
            fragmentManager = childFragmentManager,
            episodeUuid = uiState.episodeUuid,
            podcastUuid = uiState.podcastUuid,
            sourceView = args.sourceView,
            podcastColors = viewModel.uiState.value.podcastColors,
            trigger = ChatFeedbackFragment.Trigger.ResponseRating,
        )
    }

    private fun showOptionsDialog() {
        val view = view ?: return
        UiUtil.hideKeyboard(view)
        view.post {
            if (isAdded) {
                showClearChatOptionsDialog()
            }
        }
    }

    private fun showClearChatOptionsDialog() {
        val dangerColor = requireContext().getThemeColor(UR.attr.support_05)
        val dialog = OptionsDialog().setForceDarkTheme(args.sourceView == SourceView.PLAYER)
        if (!viewModel.uiState.value.isAwaitingReply) {
            dialog.addTextOption(
                titleId = LR.string.chat_history,
                imageId = IR.drawable.ic_history,
                click = viewModel::showHistory,
            )
        }
        dialog
            .addTextOption(
                titleId = LR.string.chat_clear,
                titleColor = dangerColor,
                imageId = IR.drawable.ic_delete,
                imageColor = dangerColor,
                click = viewModel::clearChat,
            )
            .show(parentFragmentManager, "chat_options")
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.trackDismissed()
        showSurveyIfEligible()
        super.onDismiss(dialog)
    }

    private fun showSurveyIfEligible() {
        val isHostGoingAway = parentFragment?.isRemoving == true ||
            activity?.isChangingConfigurations == true ||
            activity?.isFinishing == true
        if (isHostGoingAway || parentFragmentManager.isStateSaved || !viewModel.consumeSurveyEligibility()) return
        val uiState = viewModel.uiState.value
        ChatSurveyFragment.show(
            fragmentManager = parentFragmentManager,
            episodeUuid = uiState.episodeUuid,
            podcastUuid = uiState.podcastUuid,
            sourceView = args.sourceView,
            podcastColors = viewModel.uiState.value.podcastColors,
        )
    }

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        bottomSheetView()?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.isDraggable = false
            behavior.maxHeight = resources.displayMetrics.heightPixels
        }
    }

    private val ChatUiState.podcastColors
        get() = when {
            otherPlayingEpisode?.userEpisode != null -> PodcastColors.ForUserEpisode
            else -> playingPodcast?.let(::PodcastColors) ?: args.podcastColors
        }

    private val ChatToast.isUndoable get() = this != ChatToast.FeedbackThanks

    private fun ChatToast.message(resources: Resources) = when (this) {
        ChatToast.FeedbackThanks -> resources.getString(LR.string.chat_feedback_thank_you)
        ChatToast.ChatCleared -> resources.getString(LR.string.chat_cleared)
        ChatToast.ChatDeleted -> resources.getString(LR.string.chat_deleted)
        is ChatToast.QuotePlaying -> resources.getString(LR.string.chat_now_playing_episode, episodeTitle)
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String,
        val podcastTitle: String,
        val episodeTitle: String,
        val episodeDurationMs: Int,
        val sourceView: SourceView,
        val isBeta: Boolean,
        val podcastColors: PodcastColors,
    ) : Parcelable
}
