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
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.gms.cast.framework.CastButtonFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShelfBottomSheet : BaseDialogFragment() {
    @Inject lateinit var castManager: CastManager
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

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
            adapter.playable = it.second
            adapter.submitList(it.first.drop(4))
        }

        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)
        }

        binding.btnEdit.setOnClickListener {
            (activity as FragmentHostListener).showModal(ShelfFragment())
            dismiss()
        }

        CastButtonFactory.setUpMediaRouteButton(view.context, binding.mediaRouteButton)
    }

    private fun onClick(item: ShelfItem) {
        val analyticsAction: String
        when (item) {
            is ShelfItem.Effects -> {
                analyticsAction = ShelfItem.ShelfItemId.EFFECTS.analyticsValue
                EffectsFragment().show(parentFragmentManager, "effects")
            }
            is ShelfItem.Sleep -> {
                analyticsAction = ShelfItem.ShelfItemId.SLEEP.analyticsValue
                SleepFragment().show(parentFragmentManager, "sleep")
            }
            is ShelfItem.Star -> {
                analyticsAction = ShelfItem.ShelfItemId.STAR.analyticsValue
                playerViewModel.starToggle()
            }
            is ShelfItem.Share -> {
                analyticsAction = ShelfItem.ShelfItemId.SHARE.analyticsValue
                playerViewModel.shareDialog(context, parentFragmentManager)?.show()
            }
            is ShelfItem.Podcast -> {
                analyticsAction = ShelfItem.ShelfItemId.PODCAST.analyticsValue
                (activity as FragmentHostListener).closePlayer()
                val podcast = playerViewModel.podcast
                if (podcast != null) {
                    (activity as? FragmentHostListener)?.openPodcastPage(podcast.uuid)
                } else {
                    (activity as? FragmentHostListener)?.openCloudFiles()
                }
            }
            is ShelfItem.Cast -> {
                analyticsAction = ShelfItem.ShelfItemId.CAST.analyticsValue
                binding?.mediaRouteButton?.performClick()
            }
            is ShelfItem.Played -> {
                analyticsAction = ShelfItem.ShelfItemId.PLAYED.analyticsValue
                context?.let {
                    playerViewModel.markCurrentlyPlayingAsPlayed(it)?.show(parentFragmentManager, "mark_as_played")
                }
            }
            is ShelfItem.Archive -> {
                analyticsAction = ShelfItem.ShelfItemId.ARCHIVE.analyticsValue
                playerViewModel.archiveCurrentlyPlaying(resources)?.show(parentFragmentManager, "archive")
            }
            else -> {
                analyticsAction = AnalyticsProp.Value.UNKNOWN
            }
        }
        analyticsTracker.track(
            AnalyticsEvent.PLAYER_SHELF_ACTION_TAPPED,
            mapOf(AnalyticsProp.Key.FROM to AnalyticsProp.Value.OVERFLOW_MENU, AnalyticsProp.Key.ACTION to analyticsAction)
        )
        dismiss()
    }

    companion object {
        object AnalyticsProp {
            object Key {
                const val FROM = "from"
                const val ACTION = "action"
            }
            object Value {
                const val SHELF = "shelf"
                const val OVERFLOW_MENU = "overflow_menu"
                const val UNKNOWN = "unknown"
            }
        }
    }
}
