package au.com.shiftyjelly.pocketcasts.settings.notifications

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
internal class NotificationsSettingsFragment : BaseFragment(), PodcastSelectFragment.Listener {

    private val viewModel: NotificationsSettingsViewModel by viewModels()

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

            val state: NotificationsSettingsViewModel.State by viewModel.state.collectAsState()

            val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.let { data ->
                    val ringtone: Uri? = getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                    val filePath = ringtone?.toString().orEmpty()
                    viewModel.onPreferenceChanged(
                        // construct a fake item to pass the new value, otherwise I'd need to hold a reference to the original preference item
                        NotificationPreferenceType.NotificationSoundPreference(
                            title = TextResource.fromText(""),
                            notificationSound = NotificationSound(path = filePath, layoutInflater.context),
                            displayedSoundName = "",
                        ),
                    )
                }
            }

            CallOnce {
                viewModel.onShown()
            }

            NotificationsSettingsScreen(
                state = state,
                onPreferenceChanged = viewModel::onPreferenceChanged,
                onBackPressed = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onAdvancedSettingsClicked = {
                    notificationHelper.openEpisodeNotificationSettings(requireActivity())
                },
                onSelectRingtoneClicked = {
                    ringtonePickerLauncher.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                            )
                            // Select "Silent" if empty
                            runCatching {
                                it?.toUri()
                            }.getOrNull()?.let {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                            }
                        },
                    )
                },
            )
        }
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.onSelectedPodcastsChanged(newSelection)
    }

    override fun podcastSelectFragmentGetCurrentSelection() = runBlocking { viewModel.getSelectedPodcastIds() }
}
