package au.com.shiftyjelly.pocketcasts.discover.view

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Parcelable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCarouselListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCategoriesBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowChangeRegionBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCollectionListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowErrorBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowPodcastLargeListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowPodcastSmallListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowSingleEpisodeBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowSinglePodcastBinding
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastList
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.extensions.showIf
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val MAX_ROWS_SMALL_LIST = 20
private const val CURRENT_PAGE = "current_page"
private const val TOTAL_PAGES = "total_pages"
const val LIST_ID_KEY = "list_id"
const val PODCAST_UUID_KEY = "podcast_uuid"
const val EPISODE_UUID_KEY = "episode_uuid"

internal data class ChangeRegionRow(val region: DiscoverRegion)

internal class DiscoverAdapter(
    val service: ListRepository,
    val staticServerManager: StaticServerManagerImpl,
    val listener: Listener,
    val theme: Theme,
    val loadPodcastList: (String) -> Flowable<PodcastList>,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiscoverRowDiffCallback()) {
    interface Listener {
        fun onPodcastClicked(podcast: DiscoverPodcast, listUuid: String?)
        fun onPodcastSubscribe(podcast: DiscoverPodcast, listUuid: String?)
        fun onPodcastListClicked(list: NetworkLoadableList)
        fun onEpisodeClicked(episode: DiscoverEpisode, listUuid: String?)
        fun onEpisodePlayClicked(episode: DiscoverEpisode)
        fun onEpisodeStopClicked()
        fun onSearchClicked()
    }

    var onChangeRegion: (() -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    abstract class NetworkLoadableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        companion object {
            private val DEFAULT_ERROR_HANDLER = { error: Throwable ->
                Timber.e("Could not load feed ${error.message}")
            }
        }

        val recyclerView: RecyclerView? = itemView.findViewById(R.id.rowRecyclerView)
        private var loadingDisposable: Disposable? = null

        fun <T : Any> loadSingle(single: Single<T>, onSuccess: (T) -> (Unit), onError: (Throwable) -> Unit = DEFAULT_ERROR_HANDLER) {
            cancelLoading()
            loadingDisposable = single.observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = onSuccess,
                onError = onError
            )
        }

        fun <T : Any> loadFlowable(flowable: Flowable<T>, onNext: (T) -> (Unit), onError: (Throwable) -> Unit = DEFAULT_ERROR_HANDLER) {
            cancelLoading()
            loadingDisposable = flowable.observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onNext = onNext,
                onError = onError
            )
        }

        fun cancelLoading() {
            loadingDisposable?.dispose()
        }

        open fun onRestoreInstanceState(state: Parcelable?) {
            if (state != null) {
                recyclerView?.layoutManager?.onRestoreInstanceState(state)
            }
        }

        fun onSaveInstanceState(): Parcelable? {
            return recyclerView?.layoutManager?.onSaveInstanceState()
        }
    }

    interface ShowAllRow {
        val showAllButton: TextView
    }

    inner class LargeListViewHolder(val binding: RowPodcastLargeListBinding) : NetworkLoadableViewHolder(binding.root), ShowAllRow {
        val adapter = LargeListRowAdapter(listener::onPodcastClicked, listener::onPodcastSubscribe, analyticsTracker)
        override val showAllButton: TextView
            get() = binding.btnShowAll

        init {
            val linearLayoutManager = LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false)
            linearLayoutManager.initialPrefetchItemCount = 3
            recyclerView?.layoutManager = linearLayoutManager
            recyclerView?.itemAnimator = null

            recyclerView?.adapter = adapter

            adapter.showLoadingList()
        }
    }

    inner class CarouselListViewHolder(var binding: RowCarouselListBinding) : NetworkLoadableViewHolder(binding.root) {
        val adapter = CarouselListRowAdapter(null, theme, listener::onPodcastClicked, listener::onPodcastSubscribe, analyticsTracker)

        private val linearLayoutManager =
            LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false).apply {
                initialPrefetchItemCount = 1
            }

        init {
            recyclerView?.layoutManager = linearLayoutManager
            recyclerView?.itemAnimator = null
            val snapHelper = HorizontalPeekSnapHelper(0)
            snapHelper.attachToRecyclerView(recyclerView)
            snapHelper.onSnapPositionChanged = { position ->
                binding.pageIndicatorView.position = position
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PAGE_CHANGED, mapOf(CURRENT_PAGE to position, TOTAL_PAGES to adapter.itemCount))
            }

            recyclerView?.adapter = adapter
            adapter.submitList(listOf(LoadingItem()))
        }

        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState(state)
            recyclerView?.post {
                binding.pageIndicatorView.position = linearLayoutManager.findFirstVisibleItemPosition()
            }
        }
    }

    inner class SmallListViewHolder(val binding: RowPodcastSmallListBinding) : NetworkLoadableViewHolder(binding.root), ShowAllRow {
        val adapter = SmallListRowAdapter(listener::onPodcastClicked, listener::onPodcastSubscribe, analyticsTracker)

        override val showAllButton: TextView
            get() = binding.btnShowAll

        private val linearLayoutManager =
            LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false).apply {
                initialPrefetchItemCount = 2
            }

        init {
            recyclerView?.layoutManager = linearLayoutManager
            recyclerView?.itemAnimator = null
            val snapHelper = HorizontalPeekSnapHelper(0.dpToPx(itemView.context))
            snapHelper.attachToRecyclerView(recyclerView)
            snapHelper.onSnapPositionChanged = { position ->
                binding.pageIndicatorView.position = position
            }

            recyclerView?.adapter = adapter

            adapter.showLoadingList()
        }

        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState(state)
            recyclerView?.post {
                binding.pageIndicatorView.position = linearLayoutManager.findFirstVisibleItemPosition()
            }
        }
    }

    class CategoriesViewHolder(val binding: RowCategoriesBinding) : NetworkLoadableViewHolder(binding.root) {
        init {
            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, RecyclerView.VERTICAL, false)
        }
    }

    class ErrorViewHolder(val binding: RowErrorBinding) : RecyclerView.ViewHolder(binding.root)
    class ChangeRegionViewHolder(val binding: RowChangeRegionBinding) : RecyclerView.ViewHolder(binding.root)
    class SinglePodcastViewHolder(val binding: RowSinglePodcastBinding) : NetworkLoadableViewHolder(binding.root)
    class SingleEpisodeViewHolder(val binding: RowSingleEpisodeBinding) : NetworkLoadableViewHolder(binding.root)
    class CollectionListViewHolder(val binding: RowCollectionListBinding) : NetworkLoadableViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.row_podcast_large_list -> LargeListViewHolder(RowPodcastLargeListBinding.inflate(inflater, parent, false))
            R.layout.row_podcast_small_list -> SmallListViewHolder(RowPodcastSmallListBinding.inflate(inflater, parent, false))
            R.layout.row_carousel_list -> CarouselListViewHolder(RowCarouselListBinding.inflate(inflater, parent, false))
            R.layout.row_error -> ErrorViewHolder(RowErrorBinding.inflate(inflater, parent, false))
            R.layout.row_change_region -> ChangeRegionViewHolder(RowChangeRegionBinding.inflate(inflater, parent, false))
            R.layout.row_categories -> CategoriesViewHolder(RowCategoriesBinding.inflate(inflater, parent, false))
            R.layout.row_single_podcast -> SinglePodcastViewHolder(RowSinglePodcastBinding.inflate(inflater, parent, false))
            R.layout.row_single_episode -> SingleEpisodeViewHolder(RowSingleEpisodeBinding.inflate(inflater, parent, false))
            R.layout.row_collection_list -> CollectionListViewHolder(RowCollectionListBinding.inflate(inflater, parent, false))
            else -> ErrorViewHolder(RowErrorBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        val row = getItem(position)

        when (row) {
            is DiscoverRow -> {
                when (row.type) {
                    is ListType.PodcastList -> {
                        return when (row.displayStyle) {
                            is DisplayStyle.Carousel -> R.layout.row_carousel_list
                            is DisplayStyle.LargeList -> R.layout.row_podcast_large_list
                            is DisplayStyle.SmallList -> R.layout.row_podcast_small_list
                            is DisplayStyle.SinglePodcast -> R.layout.row_single_podcast
                            is DisplayStyle.CollectionList -> R.layout.row_collection_list
                            else -> R.layout.row_error
                        }
                    }
                    is ListType.EpisodeList -> {
                        return when (row.displayStyle) {
                            is DisplayStyle.SingleEpisode -> R.layout.row_single_episode
                            is DisplayStyle.CollectionList -> R.layout.row_collection_list
                            else -> R.layout.row_error
                        }
                    }
                    is ListType.Categories -> {
                        return R.layout.row_categories
                    }
                    else -> {}
                }
            }
            is ChangeRegionRow -> {
                return R.layout.row_change_region
            }
        }

        return R.layout.row_error
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageLoader = PodcastImageLoaderThemed(holder.itemView.context)
        val row = getItem(position)
        val resources = holder.itemView.resources
        if (row is DiscoverRow) {
            when (holder) {
                is LargeListViewHolder -> {
                    holder.binding.lblTitle.text = row.title.tryToLocalise(resources)
                    holder.loadFlowable(
                        loadPodcastList(row.source),
                        onNext = {
                            holder.adapter.fromListId = row.listUuid
                            holder.adapter.submitList(it.podcasts) { onRestoreInstanceState(holder) }
                        }
                    )
                    row.listUuid?.let { trackListImpression(it) }
                }
                is CarouselListViewHolder -> {
                    val featuredLimit = 5
                    val loadingFlowable: Flowable<List<Any>> = loadPodcastList(row.source)
                        .flatMap<List<Any>> { podcastList ->
                            Flowable.fromIterable(podcastList.podcasts)
                                .take(featuredLimit.toLong())
                                .concatMap {
                                    // For each podcast, we need to load its background color.
                                    val zipper: BiFunction<DiscoverPodcast, Optional<ArtworkColors>, DiscoverPodcast> = BiFunction { podcast: DiscoverPodcast, colors: Optional<ArtworkColors> ->
                                        val backgroundColor = colors.get()?.tintForDarkBg ?: 0
                                        podcast.color = backgroundColor
                                        podcast
                                    }
                                    Single.zip(Single.just(it), staticServerManager.getColorsSingle(it.uuid).subscribeOn(Schedulers.io()), zipper).toFlowable()
                                }
                                .toList().toFlowable()
                        }

                    holder.loadFlowable(
                        loadingFlowable,
                        onNext = {
                            holder.adapter.pillText = row.title.tryToLocalise(resources)
                            holder.adapter.submitList(it) { onRestoreInstanceState(holder) }
                            holder.binding.pageIndicatorView.count = it.count()
                        }
                    )

                    holder.binding.layoutSearch.setOnClickListener { listener.onSearchClicked() }
                }
                is SmallListViewHolder -> {
                    holder.binding.lblTitle.text = row.title.tryToLocalise(resources)
                    holder.loadFlowable(
                        loadPodcastList(row.source),
                        onNext = {
                            val podcasts = it.podcasts.subList(0, Math.min(MAX_ROWS_SMALL_LIST, it.podcasts.count()))
                            holder.binding.pageIndicatorView.count = Math.ceil(podcasts.count().toDouble() / SmallListRowAdapter.SmallListViewHolder.NUMBER_OF_ROWS_PER_PAGE.toDouble()).toInt()
                            holder.adapter.fromListId = row.listUuid
                            holder.adapter.submitPodcastList(podcasts) { onRestoreInstanceState(holder) }
                        }
                    )
                    row.listUuid?.let { trackListImpression(it) }
                }
                is CategoriesViewHolder -> {
                    holder.binding.lblTitle.text = row.title.tryToLocalise(resources)
                    val adapter = CategoriesListRowAdapter(listener::onPodcastListClicked)
                    holder.recyclerView?.adapter = adapter
                    holder.loadSingle(
                        service.getCategoriesList(row.source),
                        onSuccess = {
                            adapter.submitList(it) { onRestoreInstanceState(holder) }
                        }
                    )
                }
                is SinglePodcastViewHolder -> {
                    holder.loadFlowable(
                        loadPodcastList(row.source),
                        onNext = {
                            val podcast = it.podcasts.firstOrNull() ?: return@loadFlowable
                            val context = holder.itemView.context
                            val podcastTitle = podcast.title

                            holder.binding.lblTitle.text = podcastTitle
                            holder.binding.lblBody.text = it.description

                            val btnSubscribe = holder.binding.btnSubscribe
                            btnSubscribe.updateSubscribeButtonIcon(podcast.isSubscribed)
                            btnSubscribe.setOnClickListener {
                                btnSubscribe.updateSubscribeButtonIcon(true)
                                listener.onPodcastSubscribe(podcast = podcast, listUuid = row.listUuid)
                            }

                            imageLoader.loadSmallImage(podcast.uuid).into(holder.binding.imgPodcast)
                            holder.itemView.setOnClickListener {
                                row.listUuid?.let { listUuid ->
                                    trackDiscoverListPodcastTapped(listUuid, podcast.uuid)
                                    listener.onPodcastClicked(podcast, row.listUuid)
                                }
                            }

                            val lblSponsored = holder.binding.lblSponsored
                            if (row.sponsored) {
                                lblSponsored.setTextColor(context.getThemeColor(UR.attr.primary_text_02))
                                lblSponsored.text = context.getString(LR.string.discover_row_sponsored)
                            } else {
                                lblSponsored.setTextColor(context.getThemeColor(UR.attr.support_02))
                                lblSponsored.text = context.getString(LR.string.discover_row_fresh_pick)
                            }

                            row.listUuid?.let { listUuid -> trackListImpression(listUuid) }

                            val textSize = if ((podcastTitle ?: "").length < 15) 18f else 15f
                            holder.binding.lblTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                            onRestoreInstanceState(holder)
                        }
                    )
                }
                is SingleEpisodeViewHolder -> {
                    holder.loadFlowable(
                        loadPodcastList(row.source),
                        onNext = { sharedList ->
                            val episode = sharedList.episodes.firstOrNull() ?: return@loadFlowable
                            val context = holder.itemView.context

                            val binding = holder.binding
                            binding.listTitle.text = sharedList.title
                            binding.episodeTitle.text = episode.title
                            binding.podcastTitle.text = episode.podcast_title

                            // Set the play button text to either Play Trailer or Play Episode
                            val episodeType = Episode.EpisodeType.fromString(episode.type)
                            binding.btnPlay.setText(if (episodeType == Episode.EpisodeType.Trailer) LR.string.discover_button_play_trailer else LR.string.discover_button_play_episode)
                            binding.btnPlay.show()

                            imageLoader.loadSmallImage(episode.podcast_uuid).into(binding.imgPodcast)
                            val durationMs = (episode.duration ?: 0) * 1000
                            binding.duration.text = TimeHelper.getTimeDurationShortString(durationMs.toLong(), context)
                            val showDuration = durationMs > 0
                            binding.duration.showIf(showDuration)
                            binding.durationDateSeparator.showIf(showDuration)
                            binding.publishedDate.text = episode.published?.toLocalizedFormatPattern(pattern = "d MMM")
                            binding.btnPlay.setIconResource(if (episode.isPlaying) R.drawable.pause_episode else R.drawable.play_episode)
                            binding.btnPlay.setOnClickListener {

                                row.listUuid?.let { listUuid ->
                                    AnalyticsHelper.podcastEpisodePlayedFromList(listId = listUuid, podcastUuid = episode.podcast_uuid)
                                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY, mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to episode.podcast_uuid))
                                }
                                binding.btnPlay.setIconResource(if (!episode.isPlaying) R.drawable.pause_episode else R.drawable.play_episode)
                                if (episode.isPlaying) {
                                    listener.onEpisodeStopClicked()
                                } else {
                                    listener.onEpisodePlayClicked(episode)
                                }
                            }
                            sharedList.tintColors?.let { tintColors ->
                                if (tintColors.darkTintColor.isEmpty() || tintColors.lightTintColor.isEmpty()) {
                                    return@let
                                }
                                val tintColor = if (theme.isDarkTheme) Color.parseColor(tintColors.darkTintColor) else Color.parseColor(tintColors.lightTintColor)
                                binding.listTitle.setTextColor(tintColor)
                                with(binding.btnPlay) {
                                    val colorStateList = ColorStateList.valueOf(tintColor)
                                    iconTint = colorStateList
                                    strokeColor = colorStateList
                                    rippleColor = colorStateList
                                    setTextColor(colorStateList)
                                }
                            }
                            holder.itemView.setOnClickListener {
                                row.listUuid?.let { listUuid ->
                                    AnalyticsHelper.podcastEpisodeTappedFromList(listId = listUuid, podcastUuid = episode.podcast_uuid, episodeUuid = episode.uuid)
                                    analyticsTracker.track(
                                        AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
                                        mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to episode.podcast_uuid, EPISODE_UUID_KEY to episode.uuid)
                                    )
                                }
                                listener.onEpisodeClicked(episode = episode, listUuid = row.listUuid)
                            }
                            onRestoreInstanceState(holder)
                            row.listUuid?.let { listUuid -> trackListImpression(listUuid) }
                        }
                    )
                }
                is CollectionListViewHolder -> {
                    holder.loadFlowable(
                        loadPodcastList(row.source),
                        onNext = {
                            it.podcasts.firstOrNull() ?: it.episodes.firstOrNull() ?: return@loadFlowable
                            val context = holder.itemView.context
                            holder.binding.lblTitle.text = it.title?.tryToLocalise(resources)
                            holder.binding.lblBody.text = it.description

                            it.images?.let { images ->
                                val backgroundUrl = images[0].imageUrl
                                holder.binding.imgPodcast.load(backgroundUrl) {
                                    transformations(
                                        ThemedImageTintTransformation(
                                            context
                                        )
                                    )
                                }
                            }

                            holder.itemView.setOnClickListener {
                                listener.onPodcastListClicked(row)
                            }

                            it.tintColors?.let { tintColors ->
                                if (tintColors.darkTintColor.isBlank() || tintColors.lightTintColor.isBlank()) {
                                    return@let
                                }
                                val tintColor: Int = if (theme.isDarkTheme) Color.parseColor(tintColors.darkTintColor) else Color.parseColor(tintColors.lightTintColor)
                                holder.binding.lblSubtitle.setTextColor(tintColor)
                                holder.binding.imgTint.setBackgroundColor(tintColor)
                            }

                            holder.binding.highlightImage.showIf(it.collectionImageUrl != null)
                            it.collectionImageUrl?.let { url ->
                                holder.binding.highlightImage.load(url) {
                                    transformations(ThemedImageTintTransformation(context), CircleCropTransformation())
                                }
                            }

                            holder.binding.lblSubtitle.text = it.subtitle?.tryToLocalise(resources)?.uppercase(Locale.getDefault())

                            onRestoreInstanceState(holder)

                            row.listUuid?.let { listUuid -> trackListImpression(listUuid) }
                        }
                    )
                }
            }

            if (holder is ShowAllRow) {
                holder.showAllButton.setOnClickListener { listener.onPodcastListClicked(row) }
            }
        } else if (row is ChangeRegionRow) {
            val changeRegionRowViewHolder = holder as ChangeRegionViewHolder
            val chip = changeRegionRowViewHolder.binding.chip
            chip.text = row.region.name.tryToLocalise(resources)
            val context = chip.context
            val request = ImageRequest.Builder(context).data(row.region.flag).target {
                chip.chipIcon = it
            }.allowHardware(false).build()
            context.imageLoader.enqueue(request)
            chip.setOnClickListener { onChangeRegion?.invoke() }
        }
    }

    private val savedState: MutableMap<Long, Parcelable?> = mutableMapOf()

    private fun onRestoreInstanceState(holder: NetworkLoadableViewHolder) {
        holder.onRestoreInstanceState(savedState[holder.itemId])
    }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is NetworkLoadableViewHolder -> {
                holder.cancelLoading()
                savedState[holder.itemId] = holder.onSaveInstanceState()
            }
        }
    }

    private fun trackListImpression(listUuid: String) {
        AnalyticsHelper.listImpression(listUuid)
        analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_IMPRESSION, mapOf(LIST_ID_KEY to listUuid))
    }

    private fun trackDiscoverListPodcastTapped(listUuid: String, podcastUuid: String) {
        AnalyticsHelper.podcastTappedFromList(listUuid, podcastUuid)
        analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED, mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to podcastUuid))
    }
}

private class DiscoverRowDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(old: Any, new: Any): Boolean {
        return if (old is DiscoverRow && new is DiscoverRow) {
            old.source == new.source
        } else if (old is ChangeRegionRow && new is ChangeRegionRow) {
            true
        } else {
            old == new
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(old: Any, new: Any): Boolean {
        return old == new
    }
}
