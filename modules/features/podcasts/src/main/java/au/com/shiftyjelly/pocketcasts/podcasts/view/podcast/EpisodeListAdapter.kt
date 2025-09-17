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
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.rx2.asObservable
import au.com.shiftyjelly.pocketcasts.ui.R as UR

val PLAYBACK_DIFF: DiffUtil.ItemCallback<BaseEpisode> = object : DiffUtil.ItemCallback<BaseEpisode>() {
    override fun areItemsTheSame(oldItem: BaseEpisode, newItem: BaseEpisode): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: BaseEpisode, newItem: BaseEpisode): Boolean {
        return oldItem == newItem
    }

    // Return Unit to avoid flashing animation
    override fun getChangePayload(oldItem: BaseEpisode, newItem: BaseEpisode) = Unit
}

class EpisodeListAdapter(
    private val rowDataProvider: EpisodeRowDataProvider,
    private val bookmarkManager: BookmarkManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val settings: Settings,
    private val artworkContext: Element,
    private val onRowClick: (BaseEpisode) -> Unit,
    private val onSwipeAction: (BaseEpisode, SwipeAction) -> Unit,
    private val playButtonListener: PlayButton.OnClickListener,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val multiSelectHelper: MultiSelectEpisodesHelper,
    private val fragmentManager: FragmentManager,
    private val swipeButtonLayoutFactory: SwipeButtonLayoutFactory,
) : ListAdapter<BaseEpisode, RecyclerView.ViewHolder>(PLAYBACK_DIFF) {

    val disposables = CompositeDisposable()

    private var bookmarksAvailable: Boolean = false

    init {
        setHasStableIds(true)
    }

    @ColorInt
    var tintColor: Int? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.adapter_episode -> {
                val binding = AdapterEpisodeBinding.inflate(inflater, parent, false)
                EpisodeViewHolder(
                    binding = binding,
                    showArtwork = true,
                    fromListUuid = null,
                    imageRequestFactory = imageRequestFactory,
                    swipeRowActionsFactory = swipeRowActionsFactory,
                    rowDataProvider = rowDataProvider,
                    playButtonListener = playButtonListener,
                    onRowClick = { episode ->
                        if (multiSelectHelper.isMultiSelecting) {
                            binding.checkbox.isChecked = multiSelectHelper.toggle(episode)
                        } else {
                            onRowClick(episode)
                        }
                    },
                    onRowLongClick = { episode ->
                        multiSelectHelper.defaultLongPress(episode, fragmentManager)
                    },
                    onSwipeAction = onSwipeAction,
                )
            }

            R.layout.adapter_user_episode -> UserEpisodeViewHolder(
                binding = AdapterUserEpisodeBinding.inflate(inflater, parent, false),
                settings = settings,
                downloadProgressUpdates = downloadManager.progressUpdateRelay,
                playbackStateUpdates = playbackManager.playbackStateRelay,
                upNextChangesObservable = upNextQueue.changesObservable,
                imageRequestFactory = imageRequestFactory,
                swipeButtonLayoutFactory = swipeButtonLayoutFactory,
                userBookmarksObservable = bookmarkManager.findUserEpisodesBookmarksFlow().asObservable(),
                artworkContext = artworkContext,
            )

            else -> throw IllegalStateException("Unknown playable type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PodcastEpisode -> {
                val episodeHolder = holder as EpisodeViewHolder
                bindEpisodeViewHolder(episodeHolder, item, animateMultiSelection = false)
            }

            is UserEpisode -> {
                val episodeHolder = holder as UserEpisodeViewHolder
                bindUserEpisodeViewHolder(episodeHolder, item)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any?>) {
        when (val item = getItem(position)) {
            is PodcastEpisode -> {
                val episodeHolder = holder as EpisodeViewHolder
                bindEpisodeViewHolder(episodeHolder, item, animateMultiSelection = MULTI_SELECT_TOGGLE_PAYLOAD in payloads)
            }

            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun bindEpisodeViewHolder(
        holder: EpisodeViewHolder,
        episode: PodcastEpisode,
        animateMultiSelection: Boolean,
    ) {
        holder.bind(
            item = episode,
            isMultiSelectEnabled = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(episode),
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(artworkContext),
            streamByDefault = settings.streamingMode.value,
            animateMultiSelection = animateMultiSelection,
        )
    }

    private fun bindUserEpisodeViewHolder(holder: UserEpisodeViewHolder, episode: UserEpisode) {
        val tintColor = this.tintColor ?: holder.itemView.context.getThemeColor(UR.attr.primary_icon_01)
        holder.setup(
            episode = episode,
            tintColor = tintColor,
            playButtonListener = playButtonListener,
            streamByDefault = settings.streamingMode.value,
            upNextAction = settings.upNextSwipe.value,
            multiSelectEnabled = multiSelectHelper.isMultiSelecting,
            isSelected = multiSelectHelper.isSelected(episode),
            bookmarksAvailable = bookmarksAvailable,
        )
        holder.episodeRow.setOnClickListener {
            if (multiSelectHelper.isMultiSelecting) {
                holder.binding.checkbox.isChecked = multiSelectHelper.toggle(episode)
            } else {
                onRowClick(episode)
            }
        }
        holder.episodeRow.setOnLongClickListener {
            multiSelectHelper.defaultLongPress(multiSelectable = episode, fragmentManager = fragmentManager)
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
            holder.unbind()
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

    fun setBookmarksAvailable(bookmarksAvailable: Boolean) {
        this.bookmarksAvailable = bookmarksAvailable
    }
}
