package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.AutoAddSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PodcastSettingsFragment :
    BaseFragment(),
    HasBackstack {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<PodcastSettingsViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PodcastSettingsViewModel.Factory> { factory ->
                factory.create(args.uuid)
            }
        },
    )

    @Inject
    lateinit var settings: Settings

    private var navController: NavHostController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            val uiState by viewModel.uiState.collectAsState()
            val miniPlayerInset by settings.bottomInset.collectAsState(0)
            val toolbarColors = remember(theme.activeTheme) {
                ToolbarColors.podcast(args.lightTint, args.darkTint, theme)
            }
            val navController = rememberNavController()
            this.navController = navController

            PodcastSettingsPage(
                podcastTitle = args.title,
                uiState = uiState,
                toolbarColors = toolbarColors,
                navController = navController,
                onChangeNotifications = viewModel::changeNotifications,
                onChangeAutoDownload = viewModel::changeAutoDownload,
                onChangeAddToUpNext = viewModel::changeAddToUpNext,
                onChangeUpNextPosition = ::showUpNextPositionDialog,
                onChangeUpNextGlobalSettings = ::goToAutoAddSettings,
                onChangeAutoArchive = viewModel::changeAutoArchive,
                onChangeAutoArchiveAfterPlayingSetting = ::showAutoArchiveAfterPlayingDialog,
                onChangeAutoArchiveAfterInactiveSetting = ::showAutoArchiveAfterInactiveDialog,
                onChangeAutoArchiveLimitSetting = ::showAutoArchiveLimitDialog,
                onChangePlaybackEffects = viewModel::changePlaybackEffects,
                onDecrementPlaybackSpeed = viewModel::decrementPlaybackSpeed,
                onIncrementPlaybackSpeed = viewModel::incrementPlaybackSpeed,
                onChangeTrimMode = viewModel::changeTrimMode,
                onChangeTrimModeSetting = ::showTrimModeDialog,
                onChangeVolumeBoost = viewModel::changeVolumeBoost,
                onDecrementSkipFirst = viewModel::decrementSkipFirst,
                onIncrementSkipFirst = viewModel::incrementSkipFirst,
                onDecrementSkipLast = viewModel::decrementSkipLast,
                onIncrementSkipLast = viewModel::incrementSkipLast,
                onAddPodcastToPlaylists = viewModel::addPodcastToPlaylists,
                onRemovePodcastFromPlaylists = viewModel::removePodcastFromPlaylists,
                onUnfollow = ::showUnfollowDialog,
                onDismiss = {
                    @Suppress("DEPRECATION")
                    requireActivity().onBackPressed()
                },
                modifier = Modifier.padding(
                    bottom = miniPlayerInset.pxToDp(requireContext()).dp,
                ),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.navController = null
    }

    private fun showUpNextPositionDialog() {
        val currentPosition = viewModel.uiState.value?.podcast?.autoAddToUpNext
        if (currentPosition == null || childFragmentManager.findFragmentByTag("podcast-up-next") != null) {
            return
        }
        UpNextPositionFragment().show(childFragmentManager, "podcast-up-next")
    }

    private fun showTrimModeDialog() {
        val currentMode = viewModel.uiState.value?.podcast?.trimMode
        if (currentMode == null || childFragmentManager.findFragmentByTag("podcast-trim-mode") != null) {
            return
        }
        TrimModeFragment().show(childFragmentManager, "podcast-trim-mode")
    }

    private fun showAutoArchiveAfterPlayingDialog() {
        val currentValue = viewModel.uiState.value?.podcast?.autoArchiveAfterPlaying
        if (currentValue == null || childFragmentManager.findFragmentByTag("podcast-archive-after-playing") != null) {
            return
        }
        ArchiveAfterPlayingFragment().show(childFragmentManager, "podcast-archive-after-playing")
    }

    private fun showAutoArchiveAfterInactiveDialog() {
        val currentValue = viewModel.uiState.value?.podcast?.autoArchiveInactive
        if (currentValue == null || childFragmentManager.findFragmentByTag("podcast-archive-after-inactive") != null) {
            return
        }
        ArchiveAfterInactiveFragment().show(childFragmentManager, "podcast-archive-after-inactive")
    }

    private fun showAutoArchiveLimitDialog() {
        val currentValue = viewModel.uiState.value?.podcast?.autoArchiveEpisodeLimit
        if (currentValue == null || childFragmentManager.findFragmentByTag("podcast-archive-limit") != null) {
            return
        }
        ArchiveLimitFragment().show(childFragmentManager, "podcast-archive-limit")
    }

    private fun goToAutoAddSettings() {
        (requireActivity() as FragmentHostListener).addFragment(AutoAddSettingsFragment())
    }

    private fun showUnfollowDialog() {
        if (parentFragmentManager.findFragmentByTag("podcast-unfollow") != null) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val downloadedCount = viewModel.getDownloadedEpisodeCount()
            val title = when (downloadedCount) {
                0 -> getString(LR.string.are_you_sure)
                1 -> getString(LR.string.podcast_unsubscribe_downloaded_file_singular)
                else -> getString(LR.string.podcast_unsubscribe_downloaded_file_plural, downloadedCount)
            }
            ConfirmationDialog()
                .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.unsubscribe)))
                .setTitle(title)
                .setSummary(resources.getString(LR.string.podcast_unsubscribe_warning))
                .setIconId(IR.drawable.ic_failedwarning)
                .setOnConfirm {
                    viewModel.unfollow()
                    (requireActivity() as FragmentHostListener).closeToRoot()
                }
                .show(parentFragmentManager, "podcast-unfollow")
        }
    }

    override fun getBackstackCount(): Int {
        val composeNavigator = navController?.navigatorProvider?.get(ComposeNavigator::class)
        val backStackSize = composeNavigator?.backStack?.value?.size?.minus(1) ?: 0
        return backStackSize + super.getBackstackCount()
    }

    override fun onBackPressed(): Boolean {
        return navController?.popBackStack() == true || super.onBackPressed()
    }

    @Parcelize
    private class Args(
        val uuid: String,
        val title: String,
        @ColorInt val darkTint: Int,
        @ColorInt val lightTint: Int,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "PodcastSettingsFragmentArgs"

        fun newInstance(
            uuid: String,
            title: String,
            @ColorInt darkTint: Int,
            @ColorInt lightTint: Int,
        ) = PodcastSettingsFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(uuid, title, darkTint, lightTint))
        }
    }
}
