package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.FilterOptionsFragmentBinding
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_PLAYLIST_UUID = "playlist_uuid"
private const val ARG_OPTIONS_TYPE = "options_type"

@AndroidEntryPoint
class TimeOptionsFragment : BaseFragment(), CoroutineScope {
    sealed class OptionsType(val type: String) {
        object Time : OptionsType("time")
        object Downloaded : OptionsType("downloaded")
        object AudioVideo : OptionsType("audioVideo")
    }

    companion object {
        fun newInstance(playlist: Playlist, options: OptionsType): TimeOptionsFragment {
            val bundle = Bundle()
            bundle.putString(ARG_PLAYLIST_UUID, playlist.uuid)
            bundle.putString(ARG_OPTIONS_TYPE, options.type)
            val fragment = TimeOptionsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject
    lateinit var playlistManager: PlaylistManager

    val onCheckedChanged = { value: Boolean, position: Int ->
        val previousSelected = selectedPosition
        if (value) {
            selectedPosition = position
            updatedSelected(previousSelected, selectedPosition)
        }
    }

    private lateinit var options: List<FilterOption>
    private var binding: FilterOptionsFragmentBinding? = null

    var playlist: Playlist? = null
    var adapter: FilterOptionsAdapter? = null
    var selectedPosition: Int = 0
    val optionType: String
        get() = arguments?.getString(ARG_OPTIONS_TYPE) ?: OptionsType.Time.type

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

        val anytime = filterOptionForTitle(LR.string.filters_time_anytime, Playlist.ANYTIME)
        val last24Hours = filterOptionForTitle(LR.string.filters_time_24_hours, Playlist.LAST_24_HOURS)
        val last3Days = filterOptionForTitle(LR.string.filters_time_3_days, Playlist.LAST_3_DAYS)
        val lastWeek = filterOptionForTitle(LR.string.filters_time_week, Playlist.LAST_WEEK)
        val last2Weeks = filterOptionForTitle(LR.string.filters_time_2_weeks, Playlist.LAST_2_WEEKS)
        val lastMonth = filterOptionForTitle(LR.string.filters_time_month, Playlist.LAST_MONTH)

        val downloadAll = FilterOption(
            LR.string.all, false,
            { v, position ->
                playlist?.downloaded = true
                playlist?.notDownloaded = true
                onCheckedChanged(v, position)
            }
        )

        val downloadedOption = FilterOption(
            LR.string.downloaded, false,
            { v, position ->
                playlist?.downloaded = true
                playlist?.notDownloaded = false
                onCheckedChanged(v, position)
            }
        )
        val notDownloadedOption = FilterOption(
            LR.string.not_downloaded, false,
            { v, position ->
                playlist?.downloaded = false
                playlist?.notDownloaded = true
                onCheckedChanged(v, position)
            }
        )

        val allOption = FilterOption(
            LR.string.all, false,
            { v, position ->
                if (v) {
                    playlist?.audioVideo = Playlist.AUDIO_VIDEO_FILTER_ALL
                    onCheckedChanged(v, position)
                }
            }
        )
        val audioOption = FilterOption(
            LR.string.audio, false,
            { v, position ->
                if (v) {
                    playlist?.audioVideo = Playlist.AUDIO_VIDEO_FILTER_AUDIO_ONLY
                    onCheckedChanged(v, position)
                }
            }
        )
        val videoOption = FilterOption(
            LR.string.video, false,
            { v, position ->
                if (v) {
                    playlist?.audioVideo = Playlist.AUDIO_VIDEO_FILTER_VIDEO_ONLY
                    onCheckedChanged(v, position)
                }
            }
        )

        options = when (optionType) {
            OptionsType.Time.type -> listOf(anytime, last24Hours, last3Days, lastWeek, last2Weeks, lastMonth)
            OptionsType.Downloaded.type -> listOf(downloadAll, downloadedOption, notDownloadedOption)
            OptionsType.AudioVideo.type -> listOf(allOption, audioOption, videoOption)
            else -> throw IllegalStateException("Unknown options type")
        }

        val binding = binding ?: return

        val titleId = when (optionType) {
            OptionsType.Time.type -> LR.string.filters_release_date
            OptionsType.Downloaded.type -> LR.string.filters_download_options
            OptionsType.AudioVideo.type -> LR.string.filters_chip_media_type
            else -> throw IllegalStateException("Unknown options type")
        }
        binding.lblTitle.setText(titleId)

        val btnSave = binding.btnSave
        val btnClose = binding.btnClose
        val recyclerView = binding.recyclerView

        launch {
            val playlist = async(Dispatchers.Default) { playlistManager.findByUuid(requireArguments().getString(ARG_PLAYLIST_UUID)!!) }.await()!!
            this@TimeOptionsFragment.playlist = playlist

            selectedPosition = when (optionType) {
                OptionsType.Time.type -> options.indexOfFirst { it.playlistValue!! >= playlist.filterHours }
                OptionsType.Downloaded.type -> if (playlist.downloaded && playlist.notDownloaded) 0 else if (playlist.downloaded) 1 else 2
                OptionsType.AudioVideo.type -> if (playlist.audioVideo == Playlist.AUDIO_VIDEO_FILTER_ALL) 0 else if (playlist.audioVideo == Playlist.AUDIO_VIDEO_FILTER_AUDIO_ONLY) 1 else 2
                else -> throw IllegalStateException("Unknown options type")
            }

            updatedSelected(0, selectedPosition)

            val color = playlist.getColor(context)
            val filterTintColor = ThemeColor.filterInteractive01(theme.activeTheme, color)
            btnSave.backgroundTintList = ColorStateList.valueOf(filterTintColor)
            btnSave.setTextColor(ThemeColor.filterInteractive02(theme.activeTheme, color))

            val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = FilterOptionsAdapter(options, FilterOptionsAdapterType.Radio, filterTintColor)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter

            btnClose.imageTintList = ColorStateList.valueOf(filterTintColor)
        }

        btnSave.setOnClickListener {
            playlist?.let { playlist ->
                when (optionType) {
                    OptionsType.Time.type -> {
                        playlist.filterHours = options[selectedPosition].playlistValue ?: 0
                    }
                    OptionsType.Downloaded.type -> {
                        if (selectedPosition == 0) {
                            playlist.downloaded = true
                            playlist.notDownloaded = true
                        } else if (selectedPosition == 1) {
                            playlist.downloaded = true
                            playlist.notDownloaded = false
                        } else if (selectedPosition == 2) {
                            playlist.downloaded = false
                            playlist.notDownloaded = true
                        }
                    }
                    OptionsType.AudioVideo.type -> {
                        if (selectedPosition == 0) {
                            playlist.audioVideo = Playlist.AUDIO_VIDEO_FILTER_ALL
                        } else if (selectedPosition == 1) {
                            playlist.audioVideo = Playlist.AUDIO_VIDEO_FILTER_AUDIO_ONLY
                        } else if (selectedPosition == 2) {
                            playlist.audioVideo = Playlist.AUDIO_VIDEO_FILTER_VIDEO_ONLY
                        }
                    }
                }

                launch(Dispatchers.Default) {
                    playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
                    playlistManager.update(playlist)
                    launch(Dispatchers.Main) { (activity as FragmentHostListener).closeModal(this@TimeOptionsFragment) }
                }
            }
        }

        btnClose.setOnClickListener {
            (activity as FragmentHostListener).closeModal(this)
        }
    }

    private fun filterOptionForTitle(@StringRes title: Int, hours: Int): FilterOption {
        return FilterOption(title, false, onCheckedChanged, hours)
    }

    private fun updatedSelected(previousIndex: Int, newIndex: Int) {
        options.forEachIndexed { index, filterOption ->
            filterOption.isChecked = index == newIndex
        }

        adapter?.notifyItemChanged(previousIndex)
        adapter?.notifyItemChanged(newIndex)
    }
}
