package au.com.shiftyjelly.pocketcasts.discover.view

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent.DISCOVER_AD_CATEGORY_TAPPED
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCarouselListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCategoriesBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCategoryAdBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCategoryPillsBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowChangeRegionBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowCollectionListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowErrorBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowMostPopularPodcastsBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowPodcastLargeListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowPodcastSmallListBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowRemainingPodcastsByCategoryBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowSingleEpisodeBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.RowSinglePodcastBinding
import au.com.shiftyjelly.pocketcasts.discover.extensions.updateSubscribeButtonIcon
import au.com.shiftyjelly.pocketcasts.discover.util.AutoScrollHelper
import au.com.shiftyjelly.pocketcasts.discover.util.ScrollingLinearLayoutManager
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.CATEGORY_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.EPISODE_UUID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.LIST_ID_KEY
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment.Companion.PODCAST_UUID_KEY
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.CarouselSponsoredPodcast
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastList
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory.Companion.ALL_CATEGORIES_ID
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.model.SponsoredPodcast
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
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
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val MAX_ROWS_SMALL_LIST = 20
private const val CURRENT_PAGE = "current_page"
private const val TOTAL_PAGES = "total_pages"
private const val INITIAL_PREFETCH_COUNT = 1
private const val LIST_ID = "list_id"

internal data class ChangeRegionRow(val region: DiscoverRegion)
internal data class MostPopularPodcastsByCategoryRow(val listId: String?, val category: String?, val podcasts: List<DiscoverPodcast>) {
    companion object {
        private const val TITLE_CATEGORY_KEY = "[category]"
        const val TITLE_TEMPLATE = "most popular in $TITLE_CATEGORY_KEY"
    }
}
internal data class RemainingPodcastsByCategoryRow(val listId: String?, val category: String?, val podcasts: List<DiscoverPodcast>)
internal data class CategoryAdRow(val discoverRow: DiscoverRow)
internal class DiscoverAdapter(
    val context: Context,
    val service: ListRepository,
    val staticServerManager: StaticServerManagerImpl,
    val listener: Listener,
    val theme: Theme,
    loadPodcastList: (source: String, categoryId: String) -> Flowable<PodcastList>,
    val loadCarouselSponsoredPodcastList: (List<SponsoredPodcast>, categoryId: String) -> Flowable<List<CarouselSponsoredPodcast>>,
    val loadCategories: (String) -> Flowable<List<CategoryPill>>,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiscoverRowDiffCallback()) {
    interface Listener {
        fun onPodcastClicked(podcast: DiscoverPodcast, listUuid: String?)
        fun onPodcastSubscribe(podcast: DiscoverPodcast, listUuid: String?)
        fun onPodcastListClicked(list: NetworkLoadableList)
        fun onEpisodeClicked(episode: DiscoverEpisode, listUuid: String?)
        fun onEpisodePlayClicked(episode: DiscoverEpisode)
        fun onEpisodeStopClicked()
        fun onSearchClicked()
        fun onCategoryClick(selectedCategory: CategoryPill, onCategorySelectionSuccess: () -> Unit)
        fun onAllCategoriesClick(source: String, onCategorySelectionSuccess: (CategoryPill) -> Unit, onCategorySelectionCancel: () -> Unit)
        fun onClearCategoryFilterClick(source: String, onCategoryClearSuccess: (List<CategoryPill>) -> Unit)
    }

    val loadPodcastList = { source: String, categoryId: String ->
        loadPodcastList(source, categoryId).distinctUntilChanged()
    }
    var onChangeRegion: (() -> Unit)? = null

    private val imageRequestFactory = PocketCastsImageRequestFactory(context).smallSize().themed()

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
                onError = onError,
            )
        }

        fun <T : Any> loadFlowable(flowable: Flowable<T>, onNext: (T) -> (Unit), onError: (Throwable) -> Unit = DEFAULT_ERROR_HANDLER) {
            cancelLoading()
            loadingDisposable = flowable.observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onNext = onNext,
                onError = onError,
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
        val adapter = LargeListRowAdapter(context, listener::onPodcastClicked, listener::onPodcastSubscribe, analyticsTracker)
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
        private var listIdImpressionTracked = mutableListOf<String>()
        private var autoScrollHelper: AutoScrollHelper? = null
        private val scrollListener = object : OnScrollListener() {
            private var draggingStarted: Boolean = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_SETTLING -> Unit // Do nothing
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        draggingStarted = true
                        autoScrollHelper?.stopAutoScrollTimer()
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (draggingStarted) {
                            /* Start auto scroll with a delay after a manual swipe */
                            autoScrollHelper?.startAutoScrollTimer(delay = AutoScrollHelper.AUTO_SCROLL_DELAY)
                            draggingStarted = false
                        }
                    }
                }
            }
        }

        val adapter = CarouselListRowAdapter(null, theme, listener::onPodcastClicked, listener::onPodcastSubscribe, analyticsTracker)

        private val scrollingLayoutManager =
            ScrollingLinearLayoutManager(
                itemView.context,
                RecyclerView.HORIZONTAL,
                false,
            ).apply {
                initialPrefetchItemCount = INITIAL_PREFETCH_COUNT
            }

        init {
            recyclerView?.layoutManager = scrollingLayoutManager
            recyclerView?.itemAnimator = null
            recyclerView?.addOnScrollListener(scrollListener)

            autoScrollHelper = AutoScrollHelper {
                if (adapter.itemCount == 0) return@AutoScrollHelper
                val currentPosition = binding.pageIndicatorView.position
                val nextPosition = (currentPosition + 1)
                    .takeIf { it < adapter.itemCount } ?: 0
                MainScope().launch {
                    if (nextPosition > currentPosition) {
                        recyclerView?.smoothScrollToPosition(nextPosition)
                    } else {
                        /* Jump to the beginning to avoid a backward scroll animation */
                        recyclerView?.scrollToPosition(nextPosition)
                    }
                    binding.pageIndicatorView.position = nextPosition
                    trackSponsoredListImpression(nextPosition)
                    trackPageChanged(nextPosition)
                }
            }

            val snapHelper = HorizontalPeekSnapHelper(0)
            snapHelper.attachToRecyclerView(recyclerView)
            snapHelper.onSnapPositionChanged = { position ->
                /* Page just snapped, skip auto scroll */
                autoScrollHelper?.skipAutoScroll()
                binding.pageIndicatorView.position = position
                trackSponsoredListImpression(position)
                trackPageChanged(position)
            }

            recyclerView?.adapter = adapter
            adapter.submitList(listOf(LoadingItem()))

            itemView.viewTreeObserver?.apply {
                /* Stop auto scroll when app is backgrounded */
                addOnWindowFocusChangeListener { hasFocus ->
                    if (!hasFocus) autoScrollHelper?.stopAutoScrollTimer()
                }
                /* Manage auto scroll when itemView's visibility changes */
                addOnGlobalLayoutListener {
                    if (itemView.isShown) {
                        /* Start auto scroll with a delay when carousel item view is re-shown */
                        autoScrollHelper?.startAutoScrollTimer(delay = AutoScrollHelper.AUTO_SCROLL_DELAY)
                    } else {
                        autoScrollHelper?.stopAutoScrollTimer()
                    }
                }
            }
        }

        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState(state)
            recyclerView?.post {
                val position = scrollingLayoutManager.findFirstVisibleItemPosition()
                binding.pageIndicatorView.position = position
                recyclerView.scrollToPosition(position)
                trackSponsoredListImpression(position)
            }
        }

        private fun trackPageChanged(position: Int) {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_FEATURED_PAGE_CHANGED,
                mapOf(CURRENT_PAGE to position, TOTAL_PAGES to adapter.itemCount),
            )
        }

        private fun trackSponsoredListImpression(position: Int) {
            val discoverPodcast = adapter.currentList[position] as? DiscoverPodcast
            discoverPodcast?.listId?.let {
                if (listIdImpressionTracked.contains(it)) return
                FirebaseAnalyticsTracker.listImpression(it)
                analyticsTracker.track(
                    AnalyticsEvent.DISCOVER_LIST_IMPRESSION,
                    mapOf(LIST_ID to it),
                )
                listIdImpressionTracked.add(it)
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
                val row = getItem(bindingAdapterPosition) as? DiscoverRow
                row?.let {
                    analyticsTracker.track(
                        AnalyticsEvent.DISCOVER_SMALL_LIST_PAGE_CHANGED,
                        mapOf(CURRENT_PAGE to position, TOTAL_PAGES to adapter.itemCount, LIST_ID_KEY to it.inferredId()),
                    )
                }
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
    inner class CategoryPillsViewHolder(val binding: RowCategoryPillsBinding) : NetworkLoadableViewHolder(binding.root) {
        private lateinit var source: String
        private lateinit var region: String
        private lateinit var allCategories: CategoryPill
        private var mostPopularCategoriesId: List<Int> = emptyList()

        private val adapter = CategoryPillListAdapter(
            onCategoryClick = { selectedCategory, onCategorySelectionSuccess ->
                trackCategoryPillTapped(selectedCategory.discoverCategory.name, selectedCategory.discoverCategory.id)
                listener.onCategoryClick(
                    selectedCategory = selectedCategory,
                    onCategorySelectionSuccess = {
                        onCategorySelectionSuccess(listOf(allCategories.copy(isSelected = true), selectedCategory.copy(isSelected = true)))
                    },
                )
            },
            onAllCategoriesClick = { onCategorySelectionCancel, onCategorySelectionSuccess ->
                listener.onAllCategoriesClick(
                    source = source,
                    onCategorySelectionSuccess = {
                        onCategorySelectionSuccess(listOf(allCategories.copy(isSelected = true), it.copy(isSelected = true)))
                    },
                    onCategorySelectionCancel = onCategorySelectionCancel,
                )
            },
            onClearCategoryClick = {
                listener.onClearCategoryFilterClick(
                    source,
                    onCategoryClearSuccess = {
                        submitCategories(
                            categories = it,
                            source = source,
                            context = binding.root.context,
                            clearFilter = true,
                        )
                    },
                )
            },
        )
        init {
            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false)
            recyclerView?.adapter = adapter
        }
        fun submitCategories(categories: List<CategoryPill>, source: String, context: Context, clearFilter: Boolean = false, region: String? = null) {
            this.source = source
            this.allCategories = CategoryPill(DiscoverCategory(ALL_CATEGORIES_ID, context.getString(LR.string.discover_all_categories), icon = "", source = ""))

            // Reset category list when the current list is empty
            // or user asked to clear the category filter
            // or after changing region
            if (adapter.currentList.isEmpty() || clearFilter || this.region != region) {
                region?.let { this.region = it }
                val categoriesFilter = mutableListOf(allCategories)
                categoriesFilter.addAll(getMostPopularCategories(categories))
                adapter.loadCategories(categoriesFilter)
            }
        }
        fun setMostPopularCategoriesId(ids: List<Int>) {
            this.mostPopularCategoriesId = ids
        }
        private fun getMostPopularCategories(categories: List<CategoryPill>): List<CategoryPill> {
            if (mostPopularCategoriesId.isEmpty()) return categories

            return categories
                .filter { it.discoverCategory.id in mostPopularCategoriesId }
                .sortedBy { mostPopularCategoriesId.indexOf(it.discoverCategory.id) }
        }

        private fun trackCategoryPillTapped(name: String, id: Int) {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_CATEGORIES_PILL_TAPPED,
                mapOf(
                    "name" to name,
                    "region" to region,
                    "id" to id,
                ),
            )
        }
    }

    inner class MostPopularPodcastsViewHolder(val binding: RowMostPopularPodcastsBinding) : NetworkLoadableViewHolder(binding.root) {
        val adapter = MostPopularPodcastsAdapter(listener::onPodcastClicked, listener::onPodcastSubscribe, analyticsTracker)
        init {
            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, RecyclerView.HORIZONTAL, false)
            recyclerView?.adapter = adapter
            recyclerView?.itemAnimator = null
        }
    }

    inner class RemainingPodcastsByCategoryViewHolder(val binding: RowRemainingPodcastsByCategoryBinding) : NetworkLoadableViewHolder(binding.root) {
        val adapter = RemainingPodcastsAdapter(
            onPodcastClick = { podcast, listUuid ->
                listener.onPodcastClicked(podcast, listUuid)
            },
            onPodcastSubscribe = { podcast, listUuid ->
                listener.onPodcastSubscribe(podcast, listUuid)
            },
        )
        init {
            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, RecyclerView.VERTICAL, false)
            recyclerView?.adapter = adapter
            recyclerView?.itemAnimator = null
        }
    }

    class ErrorViewHolder(val binding: RowErrorBinding) : RecyclerView.ViewHolder(binding.root)
    class ChangeRegionViewHolder(val binding: RowChangeRegionBinding) : RecyclerView.ViewHolder(binding.root)
    inner class CategoryAdViewHolder(val binding: RowCategoryAdBinding) : NetworkLoadableViewHolder(binding.root) {

        private var listIdImpressionTracked = mutableListOf<String>()

        fun trackSponsoredListImpression(listId: String) {
            if (listIdImpressionTracked.contains(listId)) return

            FirebaseAnalyticsTracker.listImpression(listId)
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_LIST_IMPRESSION,
                mapOf(LIST_ID to listId),
            )
            listIdImpressionTracked.add(listId)
        }
    }
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
            R.layout.row_category_pills -> CategoryPillsViewHolder(RowCategoryPillsBinding.inflate(inflater, parent, false))
            R.layout.row_most_popular_podcasts -> MostPopularPodcastsViewHolder(RowMostPopularPodcastsBinding.inflate(inflater, parent, false))
            R.layout.row_remaining_podcasts_by_category -> RemainingPodcastsByCategoryViewHolder(RowRemainingPodcastsByCategoryBinding.inflate(inflater, parent, false))
            R.layout.row_category_ad -> CategoryAdViewHolder(RowCategoryAdBinding.inflate(inflater, parent, false))
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
                if (row.type is ListType.PodcastList) {
                    return when (row.displayStyle) {
                        is DisplayStyle.Carousel -> R.layout.row_carousel_list
                        is DisplayStyle.LargeList -> R.layout.row_podcast_large_list
                        is DisplayStyle.SmallList -> R.layout.row_podcast_small_list
                        is DisplayStyle.SinglePodcast -> R.layout.row_single_podcast
                        is DisplayStyle.CollectionList -> R.layout.row_collection_list
                        else -> R.layout.row_error
                    }
                } else if (row.type is ListType.EpisodeList) {
                    return when (row.displayStyle) {
                        is DisplayStyle.SingleEpisode -> R.layout.row_single_episode
                        is DisplayStyle.CollectionList -> R.layout.row_collection_list
                        else -> R.layout.row_error
                    }
                } else if (row.type is ListType.Categories && row.displayStyle is DisplayStyle.Pills) {
                    return R.layout.row_category_pills
                } else if (row.type is ListType.Categories && row.displayStyle is DisplayStyle.Category) {
                    return R.layout.row_categories
                }
            }
            is ChangeRegionRow -> {
                return R.layout.row_change_region
            }

            is MostPopularPodcastsByCategoryRow -> {
                return R.layout.row_most_popular_podcasts
            }

            is RemainingPodcastsByCategoryRow -> {
                return R.layout.row_remaining_podcasts_by_category
            }

            is CategoryAdRow -> {
                return R.layout.row_category_ad
            }
        }

        return R.layout.row_error
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = getItem(position)
        val resources = holder.itemView.resources
        if (row is DiscoverRow) {
            when (holder) {
                is LargeListViewHolder -> {
                    holder.binding.lblTitle.text = row.title.tryToLocalise(resources)
                    holder.loadFlowable(
                        loadPodcastList(row.source, row.inferredId()),
                        onNext = {
                            row.listUuid?.let { listUuid -> holder.adapter.setFromListId(listUuid) }
                            holder.adapter.submitList(it.podcasts) { onRestoreInstanceState(holder) }
                        },
                    )
                    row.listUuid?.let { trackListImpression(it) }
                }
                is CarouselListViewHolder -> {
                    val featuredLimit = 5

                    val loadingFlowable: Flowable<List<Any>> = loadPodcastList(row.source, row.inferredId())
                        .zipWith(loadCarouselSponsoredPodcastList(row.sponsoredPodcasts, row.inferredId()))
                        .flatMap {
                            val (featuredPodcastList, sponsoredPodcastList) = it
                            val mutableList = featuredPodcastList.podcasts
                                .take(featuredLimit)
                                .toMutableList()
                            sponsoredPodcastList.forEach { sponsoredPodcast ->
                                mutableList.addSafely(sponsoredPodcast.podcast.copy(isSponsored = true, listId = sponsoredPodcast.listId), sponsoredPodcast.position)
                            }
                            Flowable.fromIterable(mutableList.toList())
                                .concatMap { discoverPodcast ->
                                    // For each podcast, we need to load its background color.
                                    val zipper: BiFunction<DiscoverPodcast, Optional<ArtworkColors>, DiscoverPodcast> = BiFunction { podcast: DiscoverPodcast, colors: Optional<ArtworkColors> ->
                                        val backgroundColor = colors.get()?.tintForDarkBg ?: 0
                                        podcast.color = backgroundColor
                                        podcast
                                    }
                                    Single.zip(Single.just(discoverPodcast), staticServerManager.getColorsSingle(discoverPodcast.uuid).subscribeOn(Schedulers.io()), zipper).toFlowable()
                                }
                                .toList().toFlowable()
                        }

                    holder.loadFlowable(
                        loadingFlowable,
                        onNext = {
                            holder.adapter.pillText = row.title.tryToLocalise(resources)
                            holder.adapter.submitList(it) { onRestoreInstanceState(holder) }
                            holder.binding.pageIndicatorView.count = it.count()
                        },
                    )

                    if (!FeatureFlag.isEnabled(Feature.CATEGORIES_REDESIGN)) {
                        holder.binding.layoutSearch.visibility = View.VISIBLE
                        holder.binding.layoutSearch.setOnClickListener { listener.onSearchClicked() }
                    }
                }
                is SmallListViewHolder -> {
                    holder.binding.lblTitle.text = row.title.tryToLocalise(resources)
                    holder.loadFlowable(
                        loadPodcastList(row.source, row.inferredId()),
                        onNext = {
                            val podcasts = it.podcasts.subList(0, Math.min(MAX_ROWS_SMALL_LIST, it.podcasts.count()))
                            holder.binding.pageIndicatorView.count = Math.ceil(podcasts.count().toDouble() / SmallListRowAdapter.SmallListViewHolder.NUMBER_OF_ROWS_PER_PAGE.toDouble()).toInt()
                            row.listUuid?.let { listUuid -> holder.adapter.setFromListId(listUuid) }
                            holder.adapter.submitPodcastList(podcasts) { onRestoreInstanceState(holder) }
                        },
                    )
                    row.listUuid?.let { trackListImpression(it) }
                }
                is CategoriesViewHolder -> {
                    holder.binding.lblTitle.text = row.title.tryToLocalise(resources)
                    val adapter = CategoriesListRowAdapter(listener::onPodcastListClicked)
                    holder.recyclerView?.adapter = adapter
                    holder.loadSingle(
                        service.getCategoriesList(row.source),
                        onSuccess = { categories ->
                            val sortedCategories = categories.map { it.copy(name = it.name.tryToLocalise(resources)) }.sortedBy { it.name }

                            adapter.submitList(sortedCategories) {
                                onRestoreInstanceState(holder)
                            }
                        },
                    )
                }
                is CategoryPillsViewHolder -> {
                    holder.loadFlowable(
                        loadCategories(row.source),
                        onNext = { categories ->
                            val context = holder.itemView.context
                            row.mostPopularCategoriesId?.let { holder.setMostPopularCategoriesId(it) }
                            holder.submitCategories(categories, row.source, context, region = row.regionCode)
                        },
                    )

                    holder.binding.layoutSearch.setOnClickListener { listener.onSearchClicked() }
                }
                is SinglePodcastViewHolder -> {
                    holder.loadFlowable(
                        loadPodcastList(row.source, row.inferredId()),
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
                                row.listUuid?.let { listUuid -> trackDiscoverListPodcastSubscribed(listUuid, podcast.uuid) }
                            }

                            imageRequestFactory.createForPodcast(podcast.uuid).loadInto(holder.binding.imgPodcast)
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
                        },
                    )
                }
                is SingleEpisodeViewHolder -> {
                    holder.loadFlowable(
                        loadPodcastList(row.source, row.inferredId()),
                        onNext = { sharedList ->
                            val episode = sharedList.episodes.firstOrNull() ?: return@loadFlowable
                            val context = holder.itemView.context

                            val binding = holder.binding
                            binding.listTitle.text = sharedList.title
                            binding.episodeTitle.text = episode.title
                            binding.podcastTitle.text = episode.podcast_title

                            // Set the play button text to either Play Trailer or Play Episode
                            val episodeType = PodcastEpisode.EpisodeType.fromString(episode.type)
                            binding.btnPlay.setText(if (episodeType == PodcastEpisode.EpisodeType.Trailer) LR.string.discover_button_play_trailer else LR.string.discover_button_play_episode)
                            binding.btnPlay.show()

                            imageRequestFactory.createForPodcast(episode.podcast_uuid).loadInto(holder.binding.imgPodcast)
                            val durationMs = (episode.duration ?: 0) * 1000
                            binding.duration.text = TimeHelper.getTimeDurationShortString(durationMs.toLong(), context)
                            val showDuration = durationMs > 0
                            binding.duration.showIf(showDuration)
                            binding.durationDateSeparator.showIf(showDuration)
                            binding.publishedDate.text = episode.published?.toLocalizedFormatPattern(pattern = "d MMM")
                            binding.btnPlay.setIconResource(if (episode.isPlaying) R.drawable.pause_episode else R.drawable.play_episode)
                            binding.btnPlay.setOnClickListener {
                                row.listUuid?.let { listUuid ->
                                    FirebaseAnalyticsTracker.podcastEpisodePlayedFromList(listId = listUuid, podcastUuid = episode.podcast_uuid)
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
                                    FirebaseAnalyticsTracker.podcastEpisodeTappedFromList(listId = listUuid, podcastUuid = episode.podcast_uuid, episodeUuid = episode.uuid)
                                    analyticsTracker.track(
                                        AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
                                        mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to episode.podcast_uuid, EPISODE_UUID_KEY to episode.uuid),
                                    )
                                }
                                listener.onEpisodeClicked(episode = episode, listUuid = row.listUuid)
                            }
                            onRestoreInstanceState(holder)
                            row.listUuid?.let { listUuid -> trackListImpression(listUuid) }
                        },
                    )
                }
                is CollectionListViewHolder -> {
                    holder.loadFlowable(
                        loadPodcastList(row.source, row.inferredId()),
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
                                            context,
                                        ),
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
                        },
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
        } else if (row is MostPopularPodcastsByCategoryRow) {
            val categoriesViewHolder = holder as MostPopularPodcastsViewHolder
            row.category?.let {
                val localizedCategory = it.tryToLocalise(resources)
                val tittle = MostPopularPodcastsByCategoryRow.TITLE_TEMPLATE.tryToLocalise(
                    resources = resources,
                    args = listOf(localizedCategory),
                )
                categoriesViewHolder.binding.lblTitle.text = tittle
                categoriesViewHolder.binding.lblTitle.contentDescription = tittle
            }
            row.listId?.let { categoriesViewHolder.adapter.setFromListId(it) }
            categoriesViewHolder.adapter.replaceList(row.podcasts)
        } else if (row is RemainingPodcastsByCategoryRow) {
            val remainingPodcastHolder = holder as RemainingPodcastsByCategoryViewHolder
            remainingPodcastHolder.adapter.replaceList(row.podcasts)
        } else if (row is CategoryAdRow) {
            val adHolder = holder as CategoryAdViewHolder

            (holder as NetworkLoadableViewHolder).loadFlowable(
                loadPodcastList(row.discoverRow.source, row.discoverRow.inferredId()),
                onNext = {
                    val podcast = it.podcasts.firstOrNull() ?: return@loadFlowable
                    val context = adHolder.itemView.context
                    val podcastTitle = podcast.title

                    adHolder.binding.lblTitle.text = podcastTitle
                    adHolder.binding.lblBody.text = it.description

                    imageRequestFactory.createForPodcast(podcast.uuid).loadInto(adHolder.binding.imgPodcast)
                    adHolder.itemView.setOnClickListener {
                        row.discoverRow.categoryId?.let { categoryId ->
                            analyticsTracker.track(DISCOVER_AD_CATEGORY_TAPPED, mapOf(CATEGORY_ID_KEY to categoryId))
                            row.discoverRow.listUuid?.let { listUuid ->
                                FirebaseAnalyticsTracker.podcastTappedFromList(listUuid, podcast.uuid)
                                analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED, mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to podcast.uuid))
                            }
                        }
                        listener.onPodcastClicked(podcast, row.discoverRow.listUuid)
                    }

                    val btnSubscribe = adHolder.binding.btnSubscribe
                    btnSubscribe.updateSubscribeButtonIcon(podcast.isSubscribed)
                    btnSubscribe.setOnClickListener {
                        btnSubscribe.updateSubscribeButtonIcon(true)
                        listener.onPodcastSubscribe(podcast = podcast, listUuid = row.discoverRow.listUuid)
                        row.discoverRow.listUuid?.let { listUuid -> trackDiscoverListPodcastSubscribed(listUuid, podcast.uuid) }
                    }

                    val lblSponsored = adHolder.binding.lblSponsored
                    if (row.discoverRow.sponsored) {
                        lblSponsored.setTextColor(context.getThemeColor(UR.attr.primary_text_02))
                        lblSponsored.text = context.getString(LR.string.discover_row_sponsored)
                    } else {
                        lblSponsored.setTextColor(context.getThemeColor(UR.attr.support_02))
                        lblSponsored.text = context.getString(LR.string.discover_row_fresh_pick)
                    }

                    val textSize = if ((podcastTitle ?: "").length < 15) 18f else 15f
                    adHolder.binding.lblTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                    onRestoreInstanceState(adHolder)
                },
            )
            row.discoverRow.listUuid?.let { adHolder.trackSponsoredListImpression(it) }
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
        FirebaseAnalyticsTracker.listImpression(listUuid)
        analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_IMPRESSION, mapOf(LIST_ID_KEY to listUuid))
    }

    private fun trackDiscoverListPodcastTapped(listUuid: String, podcastUuid: String) {
        FirebaseAnalyticsTracker.podcastTappedFromList(listUuid, podcastUuid)
        analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED, mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to podcastUuid))
    }

    private fun trackDiscoverListPodcastSubscribed(listUuid: String, podcastUuid: String) {
        FirebaseAnalyticsTracker.podcastSubscribedFromList(listUuid, podcastUuid)
        analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED, mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to podcastUuid))
    }
}

private fun MutableList<DiscoverPodcast>.addSafely(item: DiscoverPodcast, position: Int) =
    add(min(position, count()), item)

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
