package au.com.shiftyjelly.pocketcasts.chat

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatPaywallPage
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EpisodeChatPaywallDismissedEvent
import com.automattic.eventhorizon.EpisodeChatPaywallShownEvent
import com.automattic.eventhorizon.EpisodeChatPaywallSubscribeTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ChatPaywallFragment : BaseDialogFragment() {
    companion object {
        private const val ARGS_KEY = "chat_paywall_args"

        fun newInstance(
            episodeUuid: String,
            podcastUuid: String?,
        ) = ChatPaywallFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGS_KEY, Args(episodeUuid, podcastUuid))
            }
        }
    }

    private val args get() = requireArguments().requireParcelable<Args>(ARGS_KEY)

    private val viewModel by viewModels<ChatPaywallViewModel>()

    @Inject lateinit var eventHorizon: EventHorizon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventHorizon.track(
            EpisodeChatPaywallShownEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = args.episodeUuid,
                podcastUuid = args.podcastUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
            ),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox {
            val uiState by viewModel.uiState.collectAsState()

            ChatPaywallPage(
                uiState = uiState,
                onClickClose = { dismiss() },
                onClickSubscribe = {
                    eventHorizon.track(
                        EpisodeChatPaywallSubscribeTappedEvent(
                            source = SourceView.EPISODE_DETAILS.analyticsValue,
                            episodeUuid = args.episodeUuid,
                            podcastUuid = args.podcastUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
                        ),
                    )
                    OnboardingLauncher.openOnboardingFlow(
                        requireActivity(),
                        OnboardingFlow.Upsell(OnboardingUpgradeSource.EPISODE_CHAT),
                    )
                },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        eventHorizon.track(
            EpisodeChatPaywallDismissedEvent(
                source = SourceView.EPISODE_DETAILS.analyticsValue,
                episodeUuid = args.episodeUuid,
                podcastUuid = args.podcastUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE,
            ),
        )
        super.onDismiss(dialog)
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String?,
    ) : Parcelable
}
