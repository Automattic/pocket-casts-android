package au.com.shiftyjelly.pocketcasts.chat

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatSurvey
import au.com.shiftyjelly.pocketcasts.chat.ui.rememberChatTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ChatSurveyFragment : BaseDialogFragment() {
    override val statusBarIconColor = StatusBarIconColor.Light

    companion object {
        private const val ARGS_KEY = "chat_survey_args"
        private const val TAG = "episode_chat_survey"

        fun show(
            fragmentManager: FragmentManager,
            episodeUuid: String,
            podcastUuid: String,
            sourceView: SourceView,
            podcastColors: PodcastColors,
        ) {
            if (fragmentManager.isStateSaved || fragmentManager.findFragmentByTag(TAG) != null) return
            ChatSurveyFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_KEY, Args(episodeUuid, podcastUuid, sourceView, podcastColors))
                }
            }.show(fragmentManager, TAG)
        }
    }

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private val viewModel by viewModels<ChatSurveyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onShown(args.episodeUuid, args.podcastUuid, args.sourceView)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            CompositionLocalProvider(LocalPodcastColors provides args.podcastColors) {
                val backgroundColor = MaterialTheme.theme.rememberPlayerColorsOrDefault().background01
                LaunchedEffect(backgroundColor) {
                    setDialogTint(backgroundColor.toArgb())
                }

                ChatSurvey(
                    theme = rememberChatTheme(),
                    onClickNotReally = { answer(isPositive = false) },
                    onClickYes = { answer(isPositive = true) },
                )
            }
        }
    }

    private fun answer(isPositive: Boolean) {
        viewModel.onAnswer(isPositive)
        if (isPositive) {
            showChatFeedbackThanks()
        } else {
            ChatFeedbackFragment.show(
                fragmentManager = parentFragmentManager,
                episodeUuid = args.episodeUuid,
                podcastUuid = args.podcastUuid,
                sourceView = args.sourceView,
                podcastColors = args.podcastColors,
                trigger = ChatFeedbackFragment.Trigger.SessionSurvey,
            )
        }
        dismiss()
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
    ) : Parcelable
}
