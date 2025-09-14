package au.com.shiftyjelly.pocketcasts.playlists.component

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.R
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeAvailableBinding
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.playlists.SwipeAction
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForProfile
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import java.util.UUID
import kotlinx.coroutines.rx2.asObservable

class PlaylistEpisodeAdapter(
    private val bookmarkManager: BookmarkManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val settings: Settings,
    private val onRowClick: (PlaylistEpisode) -> Unit,
    private val onSwipeAction: (PlaylistEpisode, SwipeAction) -> Unit,
    private val playButtonListener: PlayButton.OnClickListener,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val multiSelectHelper: MultiSelectEpisodesHelper,
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
            R.layout.adapter_episode_available -> {
                val binding = AdapterEpisodeAvailableBinding.inflate(inflater, parent, false)
                EpisodeAvailableViewHolder(
                    binding = binding,
                    imageRequestFactory = imageRequestFactory,
                    downloadProgressUpdates = downloadManager.progressUpdateRelay,
                    playbackStateUpdates = playbackManager.playbackStateRelay,
                    upNextChangesObservable = upNextQueue.changesObservable,
                    bookmarksObservable = bookmarkManager.findBookmarksFlow(BookmarksSortTypeForProfile.DATE_ADDED_NEWEST_TO_OLDEST)
                        .asObservable(),
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
                        @SuppressLint("NotifyDataSetChanged")
                        notifyDataSetChanged()
                    },
                )
            }

            R.layout.adapter_episode_unavailable -> EpisodeUnavailableViewHolder(
                binding = AdapterEpisodeUnavailableBinding.inflate(inflater, parent, false),
                imageRequestFactory = imageRequestFactory,
                onRowClick = onRowClick,
                onSwipeAction = onSwipeAction,
            )

            else -> throw IllegalStateException("Unknown playable type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PlaylistEpisode.Available -> bindEpisodeViewHolder(holder as EpisodeAvailableViewHolder, item)
            is PlaylistEpisode.Unavailable -> bindUnavailableEpisodeViewHolder(holder as EpisodeUnavailableViewHolder, item)
        }
    }

    private fun bindEpisodeViewHolder(holder: EpisodeAvailableViewHolder, item: PlaylistEpisode.Available) {
        holder.bind(
            episodeWrapper = item,
            isMultiSelectEnabled = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(item.episode),
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(ArtworkConfiguration.Element.Filters),
            streamByDefault = settings.streamingMode.value,
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
            is PlaylistEpisode.Available -> R.layout.adapter_episode_available
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
