package au.com.shiftyjelly.pocketcasts.settings.notifications

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.settings.R
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentNotificationSettingsBinding
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
internal class NotificationsSettingsFragment :
    BaseFragment(),
    PodcastSelectFragment.Listener,
    HasBackstack {

    private val viewModel: NotificationsSettingsViewModel by viewModels()

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        val composeView = contentWithoutConsumedInsets {
            AppThemeWithBackground(theme.activeTheme) {
                val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)

                val state: NotificationsSettingsViewModel.State by viewModel.state.collectAsState()

                CallOnce {
                    viewModel.onShown()
                }

                NotificationsSettingsScreen(
                    state = state,
                    onPreferenceChange = viewModel::onPreferenceChanged,
                    onBackPress = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    },
                    bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                    onAdvancedSettingsClick = { preference ->
                        when (preference) {
                            is NotificationPreferenceType.AdvancedSettings -> notificationHelper.openEpisodeNotificationSettings(requireActivity())
                            is NotificationPreferenceType.DailyReminderSettings -> notificationHelper.openDailyReminderNotificationSettings(requireActivity())
                            is NotificationPreferenceType.RecommendationSettings -> notificationHelper.openTrendingAndRecommendationsNotificationSettings(requireActivity())
                            is NotificationPreferenceType.NewFeaturesAndTipsSettings -> notificationHelper.openNewFeaturesAndTipsNotificationSettings(requireActivity())
                            is NotificationPreferenceType.OffersSettings -> notificationHelper.openOffersNotificationSettings(requireActivity())
                            else -> Unit
                        }
                    },
                    onSelectRingtoneClick = ::showRingtoneSelector,
                    onSelectPodcastsClick = ::showPodcastSelector,
                    onSystemNotificationsSettingsClick = {
                        viewModel.reportSystemNotificationsSettingsOpened()
                        notificationHelper.openNotificationSettings(requireActivity())
                    },
                )
            }
        }
        binding.root.addView(composeView, 0)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkNotificationPermission()
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.onSelectedPodcastsChanged(newSelection)
    }

    override fun podcastSelectFragmentGetCurrentSelection() = runBlocking { viewModel.getSelectedPodcastIds() }

    override fun onBackPressed(): Boolean {
        val isShowingPodcastSelector = childFragmentManager.findFragmentByTag(TAG_PODCAST_SELECTOR)?.let {
            childFragmentManager.beginTransaction().remove(it).commit()

            true
        } ?: false

        return isShowingPodcastSelector || super.onBackPressed()
    }

    private fun showRingtoneSelector(currentFilePath: String?) {
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
                    currentFilePath?.toUri()
                }.getOrNull()?.let {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                }
            },
        )
    }

    private fun showPodcastSelector() {
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(FragmentNotificationSettingsBinding.bind(it).fragmentContainer) { v, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout(),
                )
                v.updatePadding(
                    left = bars.left,
                    top = bars.top,
                    right = bars.right,
                    bottom = bars.bottom,
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        val fragment = PodcastSelectFragment.newInstance(
            source = PodcastSelectFragmentSource.NOTIFICATIONS,
            showToolbar = true,
        )
        childFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment,
                TAG_PODCAST_SELECTOR,
            )
            .commit()
    }

    private companion object {
        const val TAG_PODCAST_SELECTOR = "podcastSelectorFragment"
    }
}
