package au.com.shiftyjelly.pocketcasts.settings.notifications

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
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
internal class NotificationsSettingsFragment : BaseFragment(), PodcastSelectFragment.Listener {

    private val viewModel: NotificationsSettingViewModel by viewModels()

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            NotificationsSettingsScreen(
                viewModel = viewModel,
                onBackPressed = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onAdvancedSettingsClicked = {
                    notificationHelper.openEpisodeNotificationSettings(requireActivity())
                },
            )
        }
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.onSelectedPodcastsChanged(newSelection)
    }

    override fun podcastSelectFragmentGetCurrentSelection() = runCatching { runBlocking { viewModel.getSelectedPodcastIds() } }.getOrDefault(emptyList())
}