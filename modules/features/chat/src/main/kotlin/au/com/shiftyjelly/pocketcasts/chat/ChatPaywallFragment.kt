package au.com.shiftyjelly.pocketcasts.chat

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
import au.com.shiftyjelly.pocketcasts.chat.ui.ChatPaywallPage
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
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
                    OnboardingLauncher.openOnboardingFlow(
                        requireActivity(),
                        OnboardingFlow.Upsell(OnboardingUpgradeSource.UNKNOWN),
                    )
                },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String?,
    ) : Parcelable
}
