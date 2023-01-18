package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.FragmentCreateFilterChipBinding
import au.com.shiftyjelly.pocketcasts.filters.databinding.RowCreateEpisodeBinding
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcasts
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PLAYBACK_DIFF
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getStringForDuration
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreateFilterChipFragment : BaseFragment(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var _binding: FragmentCreateFilterChipBinding? = null
    val binding get() = _binding!!
    val viewModel: CreateFilterViewModel by activityViewModels()
    private var chipLayoutStartY: Float = 0f
    private var scrollToChip: View? = null

    companion object {
        fun newInstance(): CreateFilterChipFragment {
            return CreateFilterChipFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateFilterChipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val episodeAdapter = SimpleEpisodeListAdapter()

        binding.recyclerView.adapter = episodeAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(DividerItemDecoration(view.context, RecyclerView.VERTICAL))

        viewModel.playlist.observe(viewLifecycleOwner) { playlist ->
            val color = playlist.getColor(context)

            val chipPodcasts = binding.chipPodcasts
            if (playlist.allPodcasts) {
                chipPodcasts.text = getString(LR.string.filters_chip_all_your_podcasts)
                chipPodcasts.setInactiveColors(theme.activeTheme, color)
            } else {
                chipPodcasts.text = resources.getStringPluralPodcasts(playlist.podcastUuidList.count())
                chipPodcasts.setActiveColors(theme.activeTheme, color)
            }
            chipPodcasts.setOnClickListener {
                openOptionPageFrom(it, PodcastOptionsFragment.newInstance(playlist))
            }

            val chipEpisodes = binding.chipEpisodes
            val episodeOptions = playlist.episodeOptionStringIds
            if ((playlist.unplayed && playlist.partiallyPlayed && playlist.finished) || episodeOptions.isEmpty()) {
                chipEpisodes.text = getString(LR.string.filters_chip_episode_status)
                chipEpisodes.setInactiveColors(theme.activeTheme, color)
            } else {
                when {
                    episodeOptions.count() > 1 -> chipEpisodes.text = episodeOptions.joinToString { getString(it) }
                    episodeOptions.isNotEmpty() -> chipEpisodes.setText(episodeOptions.first())
                    else -> chipEpisodes.text = getString(LR.string.filters_chip_episode_status)
                }
                chipEpisodes.setActiveColors(theme.activeTheme, color)
            }
            chipEpisodes.setOnClickListener {
                openOptionPageFrom(it, EpisodeOptionsFragment.newInstance(playlist))
            }

            val chipTime = binding.chipTime
            chipTime.setText(playlist.stringForFilterHours)
            if (playlist.filterHours == 0) {
                chipTime.setInactiveColors(theme.activeTheme, color)
            } else {
                chipTime.setActiveColors(theme.activeTheme, color)
            }
            chipTime.setOnClickListener {
                openOptionPageFrom(it, TimeOptionsFragment.newInstance(playlist, TimeOptionsFragment.OptionsType.Time))
            }

            val chipDuration = binding.chipDuration
            chipDuration.text = playlist.getStringForDuration(context)
            if (playlist.filterDuration) {
                chipDuration.setActiveColors(theme.activeTheme, color)
            } else {
                chipDuration.setInactiveColors(theme.activeTheme, color)
            }
            chipDuration.setOnClickListener {
                openOptionPageFrom(it, DurationOptionsFragment.newInstance(playlist))
            }

            val chipDownload = binding.chipDownload
            val downloadOptions = playlist.downloadOptionStrings
            if (downloadOptions.isEmpty()) {
                chipDownload.setInactiveColors(theme.activeTheme, color)
                chipDownload.text = getString(LR.string.filters_chip_download_status)
            } else {
                chipDownload.text = downloadOptions.joinToString { getString(it) }
                chipDownload.setActiveColors(theme.activeTheme, color)
            }
            chipDownload.setOnClickListener {
                openOptionPageFrom(it, TimeOptionsFragment.newInstance(playlist, TimeOptionsFragment.OptionsType.Downloaded))
            }

            val chipAudioVideo = binding.chipAudioVideo
            val audioOptions = playlist.audioOptionStrings
            if (audioOptions.isEmpty()) {
                chipAudioVideo.setInactiveColors(theme.activeTheme, color)
                chipAudioVideo.text = getString(LR.string.filters_chip_media_type)
            } else {
                chipAudioVideo.text = audioOptions.joinToString { getString(it) }
                chipAudioVideo.setActiveColors(theme.activeTheme, color)
            }
            chipAudioVideo.setOnClickListener {
                openOptionPageFrom(it, TimeOptionsFragment.newInstance(playlist, TimeOptionsFragment.OptionsType.AudioVideo))
            }

            val chipStarred = binding.chipStarred
            val starred = playlist.starred
            chipStarred.text = getString(LR.string.filters_chip_starred)
            if (starred) {
                chipStarred.setActiveColors(theme.activeTheme, color)
            } else {
                chipStarred.setInactiveColors(theme.activeTheme, color)
            }
            chipStarred.setOnClickListener {
                viewModel.starredChipTapped(isCreatingFilter = true)
                scrollToChip = it
            }

            if (!playlist.isAllEpisodes) {
                val chipBox = binding.chipBox
                if (chipBox.childCount != 0) {
                    val chips = chipBox.children.toList()
                    chipBox.removeAllViews()
                    chips.forEach { binding.chipLinearLayout.addView(it) }

                    binding.lblAddMore.isVisible = true
                    binding.lblSelectFilters.isVisible = false

                    binding.chipLinearLayout.doOnLayout {
                        val x = scrollToChip?.x ?: 0f
                        binding.chipScrollView.smoothScrollTo(x.toInt(), 0)
                    }
                }

                binding.lblPreview.isVisible = true
                binding.previewDivider.isVisible = true
                viewModel.observeFilter(playlist).observe(viewLifecycleOwner) {
                    binding.recyclerView.isVisible = it.isNotEmpty()
                    binding.emptyLayout.root.isVisible = it.isEmpty()
                    episodeAdapter.submitList(it)
                }
            }
        }

        // Setup sticky header for horizontal scroll view
        binding.rootScrollView.doOnLayout { chipLayoutStartY = binding.chipScrollView.y }
        binding.rootScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY >= chipLayoutStartY) {
                binding.chipScrollView.translationY = scrollY - chipLayoutStartY
                binding.chipScrollView.elevation = 8.dpToPx(binding.root.context).toFloat()
            } else {
                binding.chipScrollView.translationY = 0f
                binding.chipScrollView.elevation = 0f
            }
        }
    }

    override fun onDestroyView() {
        scrollToChip = null
        super.onDestroyView()
        _binding = null
    }

    private fun openOptionPageFrom(optionView: View, fragment: Fragment) {
        (activity as FragmentHostListener).showModal(fragment)
        scrollToChip = optionView
        viewModel.lockedToFirstPage.value = false
    }
}

private class SimpleEpisodeListAdapter : ListAdapter<Playable, SimpleEpisodeListAdapter.ViewHolder>(PLAYBACK_DIFF) {
    class ViewHolder(val binding: RowCreateEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageLoader = PodcastImageLoaderThemed(binding.root.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowCreateEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.imageView.context
        holder.binding.lblTitle.text = item.title
        holder.binding.lblDate.text = item.publishedDate.toLocalizedFormatLongStyle()
        val timeLeft = TimeHelper.getTimeLeft(item.playedUpToMs, item.durationMs.toLong(), item.isInProgress, context)
        holder.binding.lblSubtitle.text = timeLeft.text
        holder.binding.lblSubtitle.contentDescription = timeLeft.description
        holder.imageLoader.load(item).into(holder.binding.imageView)
    }
}

private fun Chip.setActiveColors(theme: Theme.ThemeType, @ColorInt filterColor: Int) {
    val backgroundColor = ThemeColor.filterInteractive01(theme, filterColor)
    chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
    setTextColor(ThemeColor.filterInteractive02(theme, filterColor))
    chipStrokeColor = ColorStateList.valueOf(backgroundColor)
    chipStrokeWidth = 1.dpToPx(context).toFloat()
}

private fun Chip.setInactiveColors(theme: Theme.ThemeType, @ColorInt filterColor: Int) {
    val color = ThemeColor.filterInteractive06(theme, filterColor)
    chipBackgroundColor = ColorStateList.valueOf(ThemeColor.primaryUi01(theme))
    setTextColor(color)
    chipStrokeColor = ColorStateList.valueOf(color)
    chipStrokeWidth = 1.dpToPx(context).toFloat()
}
