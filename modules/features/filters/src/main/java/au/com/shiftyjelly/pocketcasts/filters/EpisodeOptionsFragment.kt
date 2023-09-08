package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.FilterOptionsFragmentBinding
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_PLAYLIST_UUID = "playlist_uuid"

@AndroidEntryPoint
class EpisodeOptionsFragment : BaseFragment(), CoroutineScope {
    companion object {
        fun newInstance(playlist: Playlist): EpisodeOptionsFragment {
            val bundle = Bundle()
            bundle.putString(ARG_PLAYLIST_UUID, playlist.uuid)
            val fragment = EpisodeOptionsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var playlistManager: PlaylistManager

    var playlist: Playlist? = null
    private var binding: FilterOptionsFragmentBinding? = null
    private var userChanged = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FilterOptionsFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.lblTitle.text = getString(LR.string.filters_chip_episode_status)

        val btnSave = binding.btnSave
        val btnClose = binding.btnClose
        val recyclerView = binding.recyclerView

        launch {
            val uuid = requireArguments().getString(ARG_PLAYLIST_UUID)!!
            Timber.d("Loading playlist $uuid")
            val playlist = playlistManager.findByUuid(uuid) ?: return@launch

            this@EpisodeOptionsFragment.playlist = playlist

            val unplayedOption = FilterOption(LR.string.unplayed, playlist.unplayed, { v, _ ->
                playlist.unplayed = v
                userChanged = true
            })
            val inProgressOption = FilterOption(LR.string.in_progress, playlist.partiallyPlayed, { v, _ ->
                playlist.partiallyPlayed = v
                userChanged = true
            })
            val playedOption = FilterOption(LR.string.played, playlist.finished, { v, _ ->
                playlist.finished = v
                userChanged = true
            })

            val color = playlist.getColor(context)
            val filterTintColor = ThemeColor.filterInteractive01(theme.activeTheme, color)
            btnSave.setBackgroundColor(filterTintColor)
            btnSave.setTextColor(ThemeColor.filterInteractive02(theme.activeTheme, color))

            val options = listOf(unplayedOption, inProgressOption, playedOption)
            val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val adapter = FilterOptionsAdapter(options, tintColor = filterTintColor)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter

            btnClose.imageTintList = ColorStateList.valueOf(filterTintColor)
        }

        btnSave.setOnClickListener {
            playlist?.let { playlist ->
                launch(Dispatchers.Default) {
                    playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED

                    val userPlaylistUpdate = if (userChanged) {
                        UserPlaylistUpdate(
                            listOf(PlaylistProperty.EpisodeStatus),
                            PlaylistUpdateSource.FILTER_EPISODE_LIST
                        )
                    } else null
                    playlistManager.update(playlist, userPlaylistUpdate)

                    launch(Dispatchers.Main) { (activity as FragmentHostListener).closeModal(this@EpisodeOptionsFragment) }
                }
            }
        }

        btnClose.setOnClickListener {
            (activity as FragmentHostListener).closeModal(this)
        }
    }
}
