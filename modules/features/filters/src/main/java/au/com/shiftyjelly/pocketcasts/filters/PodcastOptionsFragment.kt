package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import au.com.shiftyjelly.pocketcasts.filters.databinding.PodcastOptionsFragmentBinding
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.updateTint
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ARG_PLAYLIST_UUID = "playlist_uuid"

@AndroidEntryPoint
class PodcastOptionsFragment : BaseFragment(), PodcastSelectFragment.Listener, CoroutineScope {
    companion object {
        fun newInstance(playlist: Playlist): PodcastOptionsFragment {
            val bundle = Bundle()
            bundle.putString(ARG_PLAYLIST_UUID, playlist.uuid)
            val fragment = PodcastOptionsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var playlistManager: PlaylistManager

    var podcastSelection: List<String> = listOf()
    var playlist: Playlist? = null
    private var binding: PodcastOptionsFragmentBinding? = null
    private var userChanged = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PodcastOptionsFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return
        val podcastSelectDisabled = binding.podcastSelectDisabled
        val switchAllPodcasts = binding.switchAllPodcasts
        val btnSave = binding.btnSave
        val btnClose = binding.btnClose

        launch {
            val subscribedPodcasts = withContext(Dispatchers.Default) { podcastManager.findSubscribed() }.map { it.uuid }
            val playlist = withContext(Dispatchers.Default) { playlistManager.findByUuid(requireArguments().getString(ARG_PLAYLIST_UUID)!!) }!!
            this@PodcastOptionsFragment.playlist = playlist

            val color = playlist.getColor(context)

            podcastSelection = if (playlist.podcastUuidList.isEmpty() || playlist.allPodcasts) subscribedPodcasts else playlist.podcastUuidList
            podcastSelectDisabled.isVisible = playlist.allPodcasts
            switchAllPodcasts.isChecked = playlist.allPodcasts

            val fragment = PodcastSelectFragment.newInstance(
                tintColor = color,
                source = PodcastSelectFragmentSource.FILTERS
            )
            childFragmentManager.commit {
                add(R.id.podcastSelectFrame, fragment)
            }

            switchAllPodcasts.setOnCheckedChangeListener { _, isChecked ->
                podcastSelectDisabled.isVisible = isChecked
                if (!isChecked) {
                    podcastSelection = emptyList()
                    fragment.deselectAll()
                } else {
                    podcastSelection = subscribedPodcasts
                    fragment.selectAll()
                }
                userChanged = true
            }

            val filterTintColor = ThemeColor.filterInteractive01(theme.activeTheme, color)
            val filterStateList = ColorStateList.valueOf(filterTintColor)
            btnSave.backgroundTintList = filterStateList
            btnSave.setTextColor(ThemeColor.filterInteractive02(theme.activeTheme, color))
            btnClose.imageTintList = filterStateList

            val disabledColor = ThemeColor.contrast01(theme.activeTheme)
            switchAllPodcasts.updateTint(filterTintColor, disabledColor)
        }

        btnSave.setOnClickListener {
            playlist?.let { playlist ->
                playlist.podcastUuidList = podcastSelection
                playlist.allPodcasts = switchAllPodcasts.isChecked || podcastSelection.isEmpty()
                launch(Dispatchers.Default) {
                    playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED

                    val podcastSelectFragment = childFragmentManager.findFragmentById(R.id.podcastSelectFrame) as? PodcastSelectFragment
                    if (podcastSelectFragment == null) {
                        Timber.e("Could not retrieve child PodcastSelectFragment")
                    }
                    val userChangedChild = podcastSelectFragment?.userChanged() ?: false

                    val userPlaylistUpdate = if (userChanged || userChangedChild) {
                        UserPlaylistUpdate(
                            listOf(PlaylistProperty.Podcasts),
                            PlaylistUpdateSource.FILTER_EPISODE_LIST
                        )
                    } else null
                    playlistManager.update(playlist, userPlaylistUpdate)

                    launch(Dispatchers.Main) { (activity as? FragmentHostListener)?.closeModal(this@PodcastOptionsFragment) }
                }
            }
        }

        btnClose.setOnClickListener {
            (activity as FragmentHostListener).closeModal(this)
        }

        val backgroundColor = context?.getThemeColor(UR.attr.primary_ui_01) ?: Color.WHITE
        val semiTransparentBackground = androidx.core.graphics.ColorUtils.setAlphaComponent(backgroundColor, 128)
        podcastSelectDisabled.setBackgroundColor(semiTransparentBackground)
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        podcastSelection = newSelection
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return podcastSelection
    }
}
