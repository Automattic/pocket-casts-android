package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentShelfBottomSheetBinding
import au.com.shiftyjelly.pocketcasts.player.view.shelf.MenuShelfItems
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.reimagine.ShareDialogFragment
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.openUrl
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.getValue
import kotlinx.coroutines.flow.map
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ShelfBottomSheet : BaseDialogFragment() {
    @Inject lateinit var castManager: CastManager

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var chromeCastAnalytics: ChromeCastAnalytics

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    override val statusBarColor: StatusBarColor? = null
    private val episodeId: String?
        get() = arguments?.getString(ARG_EPISODE_ID)

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val viewModel: ShelfViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShelfViewModel.Factory> { factory ->
                factory.create(episodeId)
            }
        },
    )

    private var binding: FragmentShelfBottomSheetBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentShelfBottomSheetBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        setupShelfListView()

        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)
        }

        binding.btnEdit.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_STARTED)
            (activity as FragmentHostListener).showModal(ShelfFragment())
            dismiss()
        }

        CastButtonFactory.setUpMediaRouteButton(view.context, binding.mediaRouteButton)
        binding.mediaRouteButton.setOnClickListener {
            chromeCastAnalytics.trackChromeCastViewShown()
        }
    }

    private fun setupShelfListView() {
        binding?.shelfItemsComposeView?.setContent {
            AppTheme(theme.activeTheme) {
                val trimmedShelf by playerViewModel.trimmedShelfLive.asFlow()
                    .map { it.copy(it.first.drop(4), it.second) }
                    .collectAsStateWithLifecycle(null)

                trimmedShelf?.let { (shelfItems, episode) ->
                    if (episode == null) return@let
                    MenuShelfItems(
                        shelfItems = shelfItems,
                        episode = episode,
                        isEditable = false,
                        onClick = this@ShelfBottomSheet::onClick,
                    )
                }
            }
        }
    }

    private fun onClick(item: ShelfItem, enabled: Boolean) {
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

            ShelfItem.Cast -> {
                binding?.mediaRouteButton?.performClick()
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
        private const val ARG_SOURCE = "source"
        private const val ARG_EPISODE_ID = "episode_id"
        fun newInstance(
            sourceView: SourceView? = null,
            episodeId: String? = null,
        ) = ShelfBottomSheet().apply {
            arguments = bundleOf(
                ARG_SOURCE to sourceView?.analyticsValue,
                ARG_EPISODE_ID to episodeId,
            )
        }
    }
}
