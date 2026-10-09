package au.com.shiftyjelly.pocketcasts.chat

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatScreen
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
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
        val thanksMessage = stringResource(LR.string.chat_feedback_thanks)
        LaunchedEffect(snackbarHostState) {
            viewModel.feedbackThanks.collectLatest {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(thanksMessage)
            }
        }

        AppTheme(theme.activeTheme) {
            CompositionLocalProvider(LocalPodcastColors provides args.podcastColors) {
                val backgroundColor = MaterialTheme.theme.rememberPlayerColorsOrDefault().background01
                LaunchedEffect(backgroundColor) {
                    setDialogTint(backgroundColor.toArgb())
                }

                ChatScreen(
                    uiState = uiState,
                    onClickClose = { dismiss() },
                    onClickMore = ::showOptionsDialog,
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
        ChatFeedbackFragment.show(
            fragmentManager = childFragmentManager,
            episodeUuid = args.episodeUuid,
            podcastUuid = args.podcastUuid,
            sourceView = args.sourceView,
            podcastColors = args.podcastColors,
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
        OptionsDialog()
            .addTextOption(
                titleId = LR.string.chat_clear,
                imageId = IR.drawable.ic_delete,
                click = { viewModel.clearChat() },
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
        ChatSurveyFragment.show(
            fragmentManager = parentFragmentManager,
            episodeUuid = args.episodeUuid,
            podcastUuid = args.podcastUuid,
            sourceView = args.sourceView,
            podcastColors = args.podcastColors,
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
