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
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatBetaSheet
import au.com.shiftyjelly.pocketcasts.chat.ui.rememberChatTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EpisodeChatBetaSheetDismissedEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetShownEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetSignInTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ChatSignInSheetFragment : BaseDialogFragment() {
    override val statusBarIconColor = StatusBarIconColor.Light

    @Inject
    lateinit var eventHorizon: EventHorizon

    companion object {
        private const val ARGS_KEY = "chat_sign_in_args"
        private const val TAG = "episode_chat_sign_in"
        const val SIGN_IN_RESULT_KEY = "episode_chat_sign_in"

        fun show(
            fragmentManager: FragmentManager,
            episodeUuid: String,
            podcastUuid: String,
            sourceView: SourceView,
            podcastColors: PodcastColors,
        ) {
            if (fragmentManager.isStateSaved || fragmentManager.findFragmentByTag(TAG) != null) return
            ChatSignInSheetFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_KEY, Args(episodeUuid, podcastUuid, sourceView, podcastColors))
                }
            }.show(fragmentManager, TAG)
        }
    }

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private var isSigningIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            eventHorizon.track(
                EpisodeChatBetaSheetShownEvent(
                    source = args.sourceView.analyticsValue,
                    episodeUuid = args.episodeUuid,
                    podcastUuid = args.podcastUuid,
                ),
            )
        }
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

                ChatBetaSheet(
                    theme = rememberChatTheme(),
                    buttonText = stringResource(LR.string.chat_beta_sheet_sign_in),
                    onClickButton = ::signIn,
                )
            }
        }
    }

    private fun signIn() {
        if (isSigningIn) return
        isSigningIn = true
        eventHorizon.track(
            EpisodeChatBetaSheetSignInTappedEvent(
                source = args.sourceView.analyticsValue,
                episodeUuid = args.episodeUuid,
                podcastUuid = args.podcastUuid,
            ),
        )
        setFragmentResult(SIGN_IN_RESULT_KEY, Bundle.EMPTY)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!isSigningIn && activity?.isChangingConfigurations != true) {
            eventHorizon.track(
                EpisodeChatBetaSheetDismissedEvent(
                    source = args.sourceView.analyticsValue,
                    episodeUuid = args.episodeUuid,
                    podcastUuid = args.podcastUuid,
                ),
            )
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
