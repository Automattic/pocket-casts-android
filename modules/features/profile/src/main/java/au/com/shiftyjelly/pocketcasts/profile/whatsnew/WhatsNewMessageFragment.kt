package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.UiState
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class WhatsNewMessageFragment : BaseFragment() {
    private val viewModel by viewModels<WhatsNewMessageViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<WhatsNewMessageViewModel.Factory> { factory ->
                factory.create(requireNotNull(requireArguments().getString(ARG_MESSAGE_ID)))
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(themeType = theme.activeTheme) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val bottomInsetPx by viewModel.bottomInset.collectAsStateWithLifecycle()
            val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }
            when (val uiState = state) {
                UiState.Loading -> WhatsNewMessageLoadingPage(
                    onBackPress = ::close,
                )

                UiState.Missing -> LaunchedEffect(Unit) { dismiss() }

                is UiState.Loaded -> WhatsNewMessagePage(
                    message = uiState.message,
                    pages = uiState.pages,
                    bottomInset = bottomInset,
                    onBackPress = ::close,
                    onActionClick = ::perform,
                )
            }
        }
    }

    private fun perform(event: WhatsNewActionEvent) {
        val host = activity as? FragmentHostListener ?: return
        when (event) {
            WhatsNewActionEvent.OpenPodcasts -> host.openTab(VR.id.navigation_podcasts)
            WhatsNewActionEvent.OpenDiscover -> host.openTab(VR.id.navigation_discover)
            WhatsNewActionEvent.OpenUpNext -> host.openTab(VR.id.navigation_upnext)
            WhatsNewActionEvent.OpenPlaylists -> host.openTab(VR.id.navigation_filters)
            WhatsNewActionEvent.OpenProfile -> host.closeProfileToRoot()
            WhatsNewActionEvent.OpenSettings -> host.addFragment(SettingsFragment())
        }
    }

    private fun close() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    private fun dismiss() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }

    companion object {
        private const val ARG_MESSAGE_ID = "whats_new_message_id"

        fun newInstance(messageId: String) = WhatsNewMessageFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MESSAGE_ID, messageId)
            }
        }
    }
}
