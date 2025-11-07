package au.com.shiftyjelly.pocketcasts.settings.developer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.notificationstesting.NotificationsTestingFragment
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeveloperFragment : BaseFragment() {

    private val viewModel: DeveloperViewModel by viewModels()

    @Inject lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            DeveloperPage(
                onBackPress = ::onBackPress,
                onForceRefreshClick = viewModel::forceRefresh,
                onTriggerNotificationClick = viewModel::triggerNotification,
                onDeleteFirstEpisodeClick = viewModel::deleteFirstEpisode,
                onTriggerUpdateEpisodeDetails = viewModel::triggerUpdateEpisodeDetails,
                onTriggerResetEoYModalProfileBadge = viewModel::resetEoYModalProfileBadge,
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onSendCrash = viewModel::onSendCrash,
                onShowWhatsNewClick = ::onShowWhatsNewClick,
                onResetSuggestedFoldersSuggestion = viewModel::resetSuggestedFoldersSuggestion,
                onShowNotificationsTestingClick = ::onShowNotificationsTestingClick,
                onResetPlaylistsOnboarding = viewModel::resetPlaylistsOnboarding,
                onResetNotificationsPrompt = viewModel::resetNotificationsPrompt,
                onShowAppReviewPrompt = viewModel::showAppReviewPrompt,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun onBackPress() {
        activity?.onBackPressed()
    }

    private fun onShowWhatsNewClick() {
        (activity as? FragmentHostListener)?.showBottomSheet(fragment = WhatsNewFragment())
    }

    private fun onShowNotificationsTestingClick() {
        (activity as? FragmentHostListener)?.showBottomSheet(fragment = NotificationsTestingFragment())
    }
}
