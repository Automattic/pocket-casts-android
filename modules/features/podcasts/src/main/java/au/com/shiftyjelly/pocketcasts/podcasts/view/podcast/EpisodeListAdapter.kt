package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterUserEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import io.reactivex.disposables.CompositeDisposable
import au.com.shiftyjelly.pocketcasts.ui.R as UR

val PLAYBACK_DIFF: DiffUtil.ItemCallback<BaseEpisode> = object : DiffUtil.ItemCallback<BaseEpisode>() {
    override fun areItemsTheSame(oldItem: BaseEpisode, newItem: BaseEpisode): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: BaseEpisode, newItem: BaseEpisode): Boolean {
        return oldItem == newItem
    }
}

class EpisodeListAdapter(
    val downloadManager: DownloadManager,
    val playbackManager: PlaybackManager,
    val upNextQueue: UpNextQueue,
    val settings: Settings,
    val onRowClick: (BaseEpisode) -> Unit,
    val playButtonListener: PlayButton.OnClickListener,
    val imageLoader: PodcastImageLoader,
    val multiSelectHelper: MultiSelectEpisodesHelper,
    val fragmentManager: FragmentManager,
    val fromListUuid: String? = null,
    val swipeButtonLayoutFactory: SwipeButtonLayoutFactory,
) : ListAdapter<BaseEpisode, RecyclerView.ViewHolder>(PLAYBACK_DIFF) {

    val disposables = CompositeDisposable()

    init {
        setHasStableIds(true)
    }

    @ColorInt var tintColor: Int? = null
        set(value) {
            field = value
            notifyDataSetChanged()
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
                imageLoader = imageLoader,
                swipeButtonLayoutFactory = swipeButtonLayoutFactory
            )
            R.layout.adapter_user_episode -> UserEpisodeViewHolder(
                binding = AdapterUserEpisodeBinding.inflate(inflater, parent, false),
                viewMode = UserEpisodeViewHolder.ViewMode.Artwork,
                downloadProgressUpdates = downloadManager.progressUpdateRelay,
                playbackStateUpdates = playbackManager.playbackStateRelay,
                upNextChangesObservable = upNextQueue.changesObservable,
                imageLoader = imageLoader,
                swipeButtonLayoutFactory = swipeButtonLayoutFactory
            )
            else -> throw IllegalStateException("Unknown playable type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EpisodeViewHolder -> bindEpisodeViewHolder(position, holder)
            is UserEpisodeViewHolder -> bindUserEpisodeViewHolder(position, holder)
        }
    }

    private fun bindEpisodeViewHolder(position: Int, holder: EpisodeViewHolder) {
        val episode = getItem(position) as PodcastEpisode

        val tintColor = this.tintColor ?: holder.itemView.context.getThemeColor(UR.attr.primary_icon_01)
        holder.setup(episode, fromListUuid, tintColor, playButtonListener, settings.streamingMode(), settings.getUpNextSwipeAction(), multiSelectHelper.isMultiSelecting, multiSelectHelper.isSelected(episode), disposables)
        holder.episodeRow.setOnClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectHelper.toggle(episode)
            } else {
                onRowClick(episode)
            }
        }
        holder.episodeRow.setOnLongClickListener {
            multiSelectHelper.defaultLongPress(multiSelectable = episode, fragmentManager = fragmentManager)
            notifyDataSetChanged()
            true
        }
    }

    private fun bindUserEpisodeViewHolder(position: Int, holder: UserEpisodeViewHolder) {
        val userEpisode = getItem(position) as UserEpisode
        val tintColor = this.tintColor ?: holder.itemView.context.getThemeColor(UR.attr.primary_icon_01)
        holder.setup(userEpisode, tintColor, playButtonListener, settings.streamingMode(), settings.getUpNextSwipeAction(), multiSelectHelper.isMultiSelecting, multiSelectHelper.isSelected(userEpisode))
        holder.episodeRow.setOnClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectHelper.toggle(userEpisode)
            } else {
                onRowClick(userEpisode)
            }
        }
        holder.episodeRow.setOnLongClickListener {
            multiSelectHelper.defaultLongPress(multiSelectable = userEpisode, fragmentManager = fragmentManager)
            notifyDataSetChanged()
            true
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
        } else if (holder is UserEpisodeViewHolder) {
            holder.clearObservers()
        }
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return item.adapterId
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is PodcastEpisode -> R.layout.adapter_episode
            is UserEpisode -> R.layout.adapter_user_episode
            else -> throw IllegalStateException("Unknown playable type")
        }
    }
}
