package au.com.shiftyjelly.pocketcasts.playlists.component

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.R
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.podcasts.R as PodcastR

class PlaylistEpisodeAdapter(
    private val playlistType: Playlist.Type,
    private val rowDataProvider: EpisodeRowDataProvider,
    private val settings: Settings,
    private val onRowClick: (PlaylistEpisode) -> Unit,
    private val onSwipeAction: (PlaylistEpisode, SwipeAction) -> Unit,
    private val playButtonListener: PlayButton.OnClickListener,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val multiSelectHelper: MultiSelectEpisodesHelper,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val fragmentManager: FragmentManager,
) : ListAdapter<PlaylistEpisode, RecyclerView.ViewHolder>(PlaylistEpisodeDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return UUID.fromString(item.uuid).mostSignificantBits
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PodcastR.layout.adapter_episode -> {
                val binding = AdapterEpisodeBinding.inflate(inflater, parent, false)
                EpisodeAvailableViewHolder(
                    binding = binding,
                    playlistType = playlistType,
                    imageRequestFactory = imageRequestFactory,
                    swipeRowActionsFactory = swipeRowActionsFactory,
                    rowDataProvider = rowDataProvider,
                    playButtonListener = playButtonListener,
                    onRowClick = { episodeWrapper ->
                        if (multiSelectHelper.isMultiSelecting) {
                            binding.checkbox.isChecked = multiSelectHelper.toggle(episodeWrapper.episode)
                        } else {
                            onRowClick(episodeWrapper)
                        }
                    },
                    onRowLongClick = { episodeWrapper ->
                        multiSelectHelper.defaultLongPress(episodeWrapper.episode, fragmentManager)
                    },
                    onSwipeAction = onSwipeAction,
                )
            }

            R.layout.adapter_episode_unavailable -> EpisodeUnavailableViewHolder(
                binding = AdapterEpisodeUnavailableBinding.inflate(inflater, parent, false),
                imageRequestFactory = imageRequestFactory,
                swipeRowActionsFactory = swipeRowActionsFactory,
                onRowClick = onRowClick,
                onSwipeAction = onSwipeAction,
            )

            else -> throw IllegalStateException("Unknown playable type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PlaylistEpisode.Available -> {
                val episodeHolder = holder as EpisodeAvailableViewHolder
                bindEpisodeViewHolder(episodeHolder, item, animateMultiSelection = false)
            }

            is PlaylistEpisode.Unavailable -> {
                val episodeHolder = holder as EpisodeUnavailableViewHolder
                bindUnavailableEpisodeViewHolder(episodeHolder, item)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any?>) {
        when (val item = getItem(position)) {
            is PlaylistEpisode.Available -> {
                val episodeHolder = holder as EpisodeAvailableViewHolder
                bindEpisodeViewHolder(episodeHolder, item, animateMultiSelection = MULTI_SELECT_TOGGLE_PAYLOAD in payloads)
            }

            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun bindEpisodeViewHolder(
        holder: EpisodeAvailableViewHolder,
        item: PlaylistEpisode.Available,
        animateMultiSelection: Boolean,
    ) {
        holder.bind(
            item = item,
            isMultiSelectEnabled = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(item.episode),
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(ArtworkConfiguration.Element.Filters),
            streamByDefault = settings.streamingMode.value,
            animateMultiSelection = animateMultiSelection,
        )
    }

    private fun bindUnavailableEpisodeViewHolder(holder: EpisodeUnavailableViewHolder, item: PlaylistEpisode.Unavailable) {
        holder.bind(
            episodeWrapper = item,
            isMultiSelectEnabled = multiSelectHelper.isMultiSelecting,
        )
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EpisodeAvailableViewHolder) {
            holder.unbind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is PlaylistEpisode.Available -> PodcastR.layout.adapter_episode
            is PlaylistEpisode.Unavailable -> R.layout.adapter_episode_unavailable
        }
    }
}

private object PlaylistEpisodeDiffCallback : DiffUtil.ItemCallback<PlaylistEpisode>() {
    override fun areItemsTheSame(oldItem: PlaylistEpisode, newItem: PlaylistEpisode) = oldItem.uuid == newItem.uuid

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: PlaylistEpisode, newItem: PlaylistEpisode): Boolean {
        return oldItem == newItem
    }

    // Return Unit to avoid flashing animation
    override fun getChangePayload(oldItem: PlaylistEpisode, newItem: PlaylistEpisode) = Unit
}
