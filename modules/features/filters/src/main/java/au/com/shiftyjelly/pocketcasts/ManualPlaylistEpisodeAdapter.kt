package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.to.ManualEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeUnavailableViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeViewHolder
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForProfile
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import io.reactivex.disposables.CompositeDisposable
import java.util.UUID
import kotlinx.coroutines.rx2.asObservable
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class ManualPlaylistEpisodeAdapter(
    private val bookmarkManager: BookmarkManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val settings: Settings,
    private val onRowClick: (ManualEpisode) -> Unit,
    private val playButtonListener: PlayButton.OnClickListener,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val multiSelectHelper: MultiSelectEpisodesHelper,
    private val fragmentManager: FragmentManager,
    private val swipeButtonLayoutFactory: SwipeButtonLayoutFactory,
    private val artworkContext: ArtworkConfiguration.Element,
) : ListAdapter<ManualEpisode, RecyclerView.ViewHolder>(ManualEpisodeDiffCallback) {
    private val disposables = CompositeDisposable()

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
            R.layout.adapter_episode -> EpisodeViewHolder(
                binding = AdapterEpisodeBinding.inflate(inflater, parent, false),
                viewMode = EpisodeViewHolder.ViewMode.Artwork,
                downloadProgressUpdates = downloadManager.progressUpdateRelay,
                playbackStateUpdates = playbackManager.playbackStateRelay,
                upNextChangesObservable = upNextQueue.changesObservable,
                imageRequestFactory = imageRequestFactory,
                settings = settings,
                swipeButtonLayoutFactory = swipeButtonLayoutFactory,
                artworkContext = artworkContext,
            )

            R.layout.adapter_episode_unavailable -> EpisodeUnavailableViewHolder(
                binding = AdapterEpisodeUnavailableBinding.inflate(inflater, parent, false),
                imageRequestFactory = imageRequestFactory,
            )

            else -> throw IllegalStateException("Unknown playable type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ManualEpisode.Available -> bindEpisodeViewHolder(holder as EpisodeViewHolder, item)
            is ManualEpisode.Unavailable -> bindUnavailableEpisodeViewHolder(holder as EpisodeUnavailableViewHolder, item)
        }
    }

    private fun bindEpisodeViewHolder(holder: EpisodeViewHolder, item: ManualEpisode.Available) {
        val episode = item.episode
        holder.setup(
            episode = episode,
            fromListUuid = null,
            tintColor = holder.itemView.context.getThemeColor(UR.attr.primary_icon_01),
            playButtonListener = playButtonListener,
            streamByDefault = settings.streamingMode.value,
            upNextAction = settings.upNextSwipe.value,
            multiSelectEnabled = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(episode),
            disposables = disposables,
            bookmarksObservable = bookmarkManager.findBookmarksFlow(BookmarksSortTypeForProfile.DATE_ADDED_NEWEST_TO_OLDEST).asObservable(),
            bookmarksAvailable = true,
        )

        holder.episodeRow.setOnClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectHelper.toggle(episode)
            } else {
                onRowClick(item)
            }
        }

        holder.episodeRow.setOnLongClickListener {
            multiSelectHelper.defaultLongPress(multiSelectable = episode, fragmentManager = fragmentManager)
            notifyDataSetChanged()
            true
        }
    }

    private fun bindUnavailableEpisodeViewHolder(holder: EpisodeUnavailableViewHolder, item: ManualEpisode.Unavailable) {
        val episode = item.episode
        holder.bind(
            episodeUuid = episode.episodeUuid,
            podcastUuid = episode.podcastUuid,
            title = episode.title,
            publishedAt = episode.publishedAt,
        )
        holder.binding.episodeRow.setOnClickListener {
            onRowClick(item)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        if (holder is EpisodeViewHolder) {
            holder.clearObservers()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is ManualEpisode.Available -> R.layout.adapter_episode
            is ManualEpisode.Unavailable -> R.layout.adapter_episode_unavailable
        }
    }
}

private object ManualEpisodeDiffCallback : DiffUtil.ItemCallback<ManualEpisode>() {
    override fun areItemsTheSame(oldItem: ManualEpisode, newItem: ManualEpisode) = oldItem.uuid == newItem.uuid

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ManualEpisode, newItem: ManualEpisode): Boolean {
        return oldItem == newItem
    }

    // Return Unit to avoid flashing animation
    override fun getChangePayload(oldItem: ManualEpisode, newItem: ManualEpisode) = Unit
}
