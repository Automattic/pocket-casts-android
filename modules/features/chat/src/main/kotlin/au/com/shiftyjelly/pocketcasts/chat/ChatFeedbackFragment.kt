package au.com.shiftyjelly.pocketcasts.chat

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatFeedbackForm
import au.com.shiftyjelly.pocketcasts.chat.ui.rememberChatTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.startActivityViewUrl
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EpisodeChatFeedbackTriggerType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ChatFeedbackFragment : BaseDialogFragment() {
    override val statusBarIconColor = StatusBarIconColor.Light

    companion object {
        private const val ARGS_KEY = "chat_feedback_args"
        private const val TAG = "episode_chat_feedback"

        fun show(
            fragmentManager: FragmentManager,
            episodeUuid: String,
            podcastUuid: String,
            sourceView: SourceView,
            podcastColors: PodcastColors,
            trigger: Trigger,
        ) {
            if (fragmentManager.isStateSaved || fragmentManager.findFragmentByTag(TAG) != null) return
            ChatFeedbackFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_KEY, Args(episodeUuid, podcastUuid, sourceView, podcastColors, trigger))
                }
            }.show(fragmentManager, TAG)
        }
    }

    enum class Trigger(val analyticsValue: EpisodeChatFeedbackTriggerType) {
        SessionSurvey(EpisodeChatFeedbackTriggerType.SessionSurvey),
    }

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private val viewModel by viewModels<ChatFeedbackViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onShown(args.episodeUuid, args.podcastUuid, args.sourceView, args.trigger.analyticsValue)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AppTheme(theme.activeTheme) {
            CompositionLocalProvider(LocalPodcastColors provides args.podcastColors) {
                val backgroundColor = MaterialTheme.theme.rememberPlayerColorsOrDefault().background01
                LaunchedEffect(backgroundColor) {
                    setDialogTint(backgroundColor.toArgb())
                }

                ChatFeedbackForm(
                    reason = uiState.reason,
                    details = uiState.details,
                    showDetails = uiState.showDetails,
                    canSubmit = uiState.canSubmit,
                    onSelectReason = viewModel::onReasonSelected,
                    onDetailsChange = viewModel::onDetailsChange,
                    onSubmit = ::submit,
                    onClickLearnMore = { context?.startActivityViewUrl(Settings.INFO_PRIVACY_URL) },
                    theme = rememberChatTheme(),
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun submit() {
        if (viewModel.submit()) {
            showChatFeedbackThanks()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (activity?.isChangingConfigurations != true) {
            viewModel.onDismissed()
        }
        super.onDismiss(dialog)
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String,
        val sourceView: SourceView,
        val podcastColors: PodcastColors,
        val trigger: Trigger,
    ) : Parcelable
}
