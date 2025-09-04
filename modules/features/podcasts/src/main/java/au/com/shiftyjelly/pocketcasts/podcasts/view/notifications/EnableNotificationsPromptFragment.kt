package au.com.shiftyjelly.pocketcasts.podcasts.view.notifications

import android.Manifest
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.PermissionChecker
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.notifications.EnableNotificationsPromptViewModel
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class EnableNotificationsPromptFragment : BaseDialogFragment() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val viewModel: EnableNotificationsPromptViewModel by viewModels()

    companion object {
        fun newInstance(): EnableNotificationsPromptFragment {
            return EnableNotificationsPromptFragment()
        }
    }

    private val permissionRequester = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.reportNotificationRequestResult(granted)
        if (!granted) {
            viewModel.handleDismissedByUser()
        }
        isFinalizingActionUsed = true
        dismiss()
    }

    private var isFinalizingActionUsed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        LaunchedEffect(Unit) {
            viewModel.messagesFlow.collect {
                when (it) {
                    is EnableNotificationsPromptViewModel.UiMessage.RequestPermission -> requestPermission()
                    is EnableNotificationsPromptViewModel.UiMessage.Dismiss -> finalizeAndDismiss()
                }
            }
        }

        CallOnce {
            viewModel.reportShown()
        }

        val state = viewModel.stateFlow.collectAsStateWithLifecycle().value

        DialogBox {
            when (state) {
                is EnableNotificationsPromptViewModel.UiState.NewOnboarding -> {
                    EnableNotificationsPromptScreenNewOnboarding(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        onCtaClick = viewModel::handleCtaClick,
                        showNewsletterSection = state.showNewsletterOptIn,
                        isNewsletterSelected = state.subscribedToNewsletter,
                        isNotificationSelected = state.notificationsEnabled,
                        onNotificationChange = viewModel::changeNotificationsEnabled,
                        onNewsletterChange = viewModel::changeNewsletterSubscription,
                    )
                }

                is EnableNotificationsPromptViewModel.UiState.PreNewOnboarding -> {
                    EnableNotificationsPromptScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onCtaClick = viewModel::handleCtaClick,
                        onDismissClick = ::finalizeAndDismiss,
                    )
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!requireActivity().isChangingConfigurations && !isFinalizingActionUsed) {
            viewModel.handleDismissedByUser()
        }
    }

    private fun finalizeAndDismiss() {
        isFinalizingActionUsed = true
        viewModel.handleDismissedByUser()
        dismiss()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                PermissionChecker.checkSelfPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED -> Unit
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    notificationHelper.openNotificationSettings(requireActivity())
                }

                else -> {
                    permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                    viewModel.reportNotificationsOptInShown()
                }
            }
        }
    }
}
