package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentShelfBottomSheetBinding
import au.com.shiftyjelly.pocketcasts.player.view.ShelfFragment.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.gms.cast.framework.CastButtonFactory
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShelfBottomSheet : BaseDialogFragment() {
    @Inject lateinit var castManager: CastManager
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Inject lateinit var chromeCastAnalytics: ChromeCastAnalytics
    @Inject lateinit var playbackManager: PlaybackManager

    override val statusBarColor: StatusBarColor? = null

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val adapter = ShelfAdapter(editable = false, listener = this::onClick, dragListener = null)
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

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        playerViewModel.trimmedShelfLive.observe(viewLifecycleOwner) {
            adapter.episode = it.second
            adapter.submitList(it.first.drop(4))
        }

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

    private fun onClick(item: ShelfItem) {
        when (item) {
            is ShelfItem.Effects -> {
                EffectsFragment().show(parentFragmentManager, "effects")
            }
            is ShelfItem.Sleep -> {
                SleepFragment().show(parentFragmentManager, "sleep")
            }
            is ShelfItem.Star -> {
                playerViewModel.starToggle()
            }
            is ShelfItem.Share -> {
                ShareFragment().show(parentFragmentManager, "sleep")
            }
            is ShelfItem.Podcast -> {
                (activity as FragmentHostListener).closePlayer()
                val podcast = playerViewModel.podcast
                if (podcast != null) {
                    (activity as? FragmentHostListener)?.openPodcastPage(podcast.uuid)
                } else {
                    (activity as? FragmentHostListener)?.openCloudFiles()
                }
            }
            is ShelfItem.Cast -> {
                binding?.mediaRouteButton?.performClick()
            }
            is ShelfItem.Played -> {
                context?.let {
                    playerViewModel.markCurrentlyPlayingAsPlayed(it)?.show(parentFragmentManager, "mark_as_played")
                }
            }
            is ShelfItem.Archive -> {
                playerViewModel.archiveCurrentlyPlaying(resources)?.show(parentFragmentManager, "archive")
            }
            is ShelfItem.Bookmark -> {
                (parentFragment as? PlayerHeaderFragment)?.onAddBookmarkClick(item)
            }
            ShelfItem.Download -> {
                Timber.e("Unexpected click on ShelfItem.Download")
            }
        }
        analyticsTracker.track(
            AnalyticsEvent.PLAYER_SHELF_ACTION_TAPPED,
            mapOf(AnalyticsProp.Key.FROM to AnalyticsProp.Value.OVERFLOW_MENU, AnalyticsProp.Key.ACTION to item.analyticsValue)
        )
        dismiss()
    }
}
