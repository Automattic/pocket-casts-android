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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast.AutoAddUpNext
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.AutoAddSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.FilterSelectFragment
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastSettingsFragment :
    BaseFragment(),
    FilterSelectFragment.Listener {
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

            PodcastSettingsPage(
                podcastTitle = args.title,
                uiState = uiState,
                toolbarColors = toolbarColors,
                onChangeNotifications = viewModel::changeNotifications,
                onChangeAutoDownload = viewModel::changeAutoDownload,
                onChangeAddToUpNext = viewModel::changeAddToUpNext,
                onChangeUpNextPosition = ::showUpNextPositionDialog,
                onChangeUpNextGlobalSettings = ::goToAutoAddSettings,
                onChangeAutoArchiveSettings = {
                    // TODO: Migrate to compose nav graph page and remove callback
                    (requireActivity() as FragmentHostListener).addFragment(PodcastAutoArchiveFragment.newInstance(args.uuid, toolbarColors))
                },
                onChangeAutoArchive = viewModel::changeAutoArchive,
                onChangeAutoArchiveAfterPlaying = viewModel::changeAutoArchiveAfterPlaying,
                onChangeAutoArchiveAfterInactive = viewModel::changeAutoArchiveAfterInactive,
                onChangeAutoArchiveLimit = viewModel::changeAutoArchiveLimit,
                onChangePlaybackEffectsSettings = {
                    // TODO: Migrate to compose nav graph page and remove callback
                    (requireActivity() as FragmentHostListener).addFragment(PodcastEffectsFragment.newInstance(args.uuid))
                },
                onChangePlaybackEffects = viewModel::changePlaybackEffects,
                onDecrementPlaybackSpeed = viewModel::decrementPlaybackSpeed,
                onIncrementPlaybackSpeed = viewModel::incrementPLaybackSpeed,
                onChangeTrimSilenceMode = viewModel::changeTrimSilenceMode,
                onChangeVolumeBoost = viewModel::changeVolumeBoost,
                onDecrementSkipFirst = viewModel::decrementSkipFirst,
                onIncrementSkipFirst = viewModel::incrementSkipFirst,
                onDecrementSkipLast = viewModel::decrementSkipLast,
                onIncrementSkipLast = viewModel::incrementSkipLast,
                onAddPodcastToPlaylist = viewModel::addPodcastToPlaylist,
                onRemovePodcastFromPlaylist = viewModel::removePodcastFromPlaylist,
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

    private fun showUpNextPositionDialog() {
        val currentPosition = viewModel.uiState.value?.podcast?.autoAddToUpNext
        if (currentPosition == null || parentFragmentManager.findFragmentByTag("podcast-up-next") != null) {
            return
        }
        OptionsDialog()
            .setTitle(getString(LR.string.podcast_settings_position))
            .addCheckedOption(
                titleId = LR.string.play_last,
                checked = currentPosition == AutoAddUpNext.PLAY_LAST,
                click = { viewModel.changeAddToUpNext(AutoAddUpNext.PLAY_LAST) },
            )
            .addCheckedOption(
                titleId = LR.string.play_next,
                checked = currentPosition == AutoAddUpNext.PLAY_NEXT,
                click = { viewModel.changeAddToUpNext(AutoAddUpNext.PLAY_NEXT) },
            )
            .show(parentFragmentManager, "podcast-up-next")
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

    override fun filterSelectFragmentSelectionChanged(newSelection: List<String>) {
        // TODO: Remove once fragment is migrated
    }

    override fun filterSelectFragmentGetCurrentSelection(): List<String> {
        // TODO: Remove once fragment is migrated
        return viewModel.uiState.value?.playlists?.map { it.uuid }.orEmpty()
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
