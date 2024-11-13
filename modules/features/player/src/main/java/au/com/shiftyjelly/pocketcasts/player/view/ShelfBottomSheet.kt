package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.view.shelf.ShelfBottomSheetPage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.openUrl
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.getValue
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ShelfBottomSheet : BaseDialogFragment() {
    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    private val episodeId: String
        get() = requireNotNull(arguments?.getString(ARG_EPISODE_ID))

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val shelfViewModel: ShelfViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShelfViewModel.Factory> { factory ->
                factory.create(
                    episodeId = episodeId,
                    isEditable = false,
                )
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)
        }
        AppTheme(theme.activeTheme) {
            ShelfBottomSheetPage(
                shelfViewModel = shelfViewModel,
                playerViewModel = playerViewModel,
                onEditButtonClick = {
                    (activity as FragmentHostListener).showModal(ShelfFragment.newInstance(episodeId))
                    dismiss()
                },
                onShelfItemClick = this@ShelfBottomSheet::onClick,
            )
        }
    }

    private fun onClick(
        item: ShelfItem,
        enabled: Boolean,
    ) {
        when (item) {
            ShelfItem.Effects -> {
                EffectsFragment().show(parentFragmentManager, "effects")
            }

            ShelfItem.Sleep -> {
                SleepFragment().show(parentFragmentManager, "sleep")
            }

            ShelfItem.Star -> {
                playerViewModel.starToggle()
            }

            ShelfItem.Transcript -> {
                if (!enabled) {
                    showSnackBar(text = getString(LR.string.transcript_error_not_available))
                } else {
                    playerViewModel.openTranscript()
                }
            }

            ShelfItem.Share -> {
                val podcast = playerViewModel.podcast ?: return
                val episode = playerViewModel.episode as? PodcastEpisode ?: return
                ShareDialogFragment
                    .newThemedInstance(podcast, episode, theme, SourceView.BOTTOM_SHELF)
                    .show(parentFragmentManager, "share_sheet")
            }

            ShelfItem.Podcast -> {
                (activity as FragmentHostListener).closePlayer()
                val podcast = playerViewModel.podcast
                if (podcast != null) {
                    (activity as? FragmentHostListener)?.openPodcastPage(podcast.uuid, SourceView.BOTTOM_SHELF.analyticsValue)
                } else {
                    (activity as? FragmentHostListener)?.openCloudFiles()
                }
            }

            ShelfItem.Cast -> { // Do nothing, handled in ShelfBottomSheetPage
            }

            ShelfItem.Played -> {
                context?.let {
                    playerViewModel.markCurrentlyPlayingAsPlayed(it)?.show(parentFragmentManager, "mark_as_played")
                }
            }

            ShelfItem.Archive -> {
                playerViewModel.archiveCurrentlyPlaying(resources)?.show(parentFragmentManager, "archive")
            }

            ShelfItem.Bookmark -> {
                (parentFragment as? PlayerHeaderFragment)?.onAddBookmarkClick(OnboardingUpgradeSource.OVERFLOW_MENU)
            }

            ShelfItem.Report -> {
                openUrl(settings.getReportViolationUrl())
            }

            ShelfItem.Download -> {
                playerViewModel.handleDownloadClickFromPlaybackActions(
                    onDownloadStart = {
                        showSnackBar(text = getString(LR.string.episode_queued_for_download))
                    },
                    onDeleteStart = {
                        showSnackBar(text = getString(LR.string.episode_was_removed))
                    },
                )
            }
        }
        if (enabled) {
            analyticsTracker.track(
                AnalyticsEvent.PLAYER_SHELF_ACTION_TAPPED,
                mapOf(AnalyticsProp.Key.FROM to AnalyticsProp.Value.OVERFLOW_MENU, AnalyticsProp.Key.ACTION to item.analyticsValue),
            )
        }
        dismiss()
    }

    private fun showSnackBar(text: CharSequence) {
        parentFragment?.view?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.LIGHT))
                .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.LIGHT))
                .show()
        }
    }

    companion object {
        private const val ARG_EPISODE_ID = "episode_id"
        fun newInstance(
            episodeId: String,
        ) = ShelfBottomSheet().apply {
            arguments = bundleOf(
                ARG_EPISODE_ID to episodeId,
            )
        }
    }
}
