package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.discoverListShowAllTapped
import au.com.shiftyjelly.pocketcasts.analytics.discoverShowAllTapped
import au.com.shiftyjelly.pocketcasts.discover.databinding.FragmentDiscoverBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverViewModel
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.PodcastList
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.extensions.quickScrollToTop
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable

@AndroidEntryPoint
class DiscoverFragment :
    BaseFragment(),
    DiscoverAdapter.Listener,
    RegionSelectFragment.Listener,
    TopScrollable {

    @Inject lateinit var settings: Settings

    @Inject lateinit var staticServiceManager: StaticServiceManagerImpl

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    private val viewModel: DiscoverViewModel by viewModels()
    private var adapter: DiscoverAdapter? = null
    private var binding: FragmentDiscoverBinding? = null

    override fun onPause() {
        super.onPause()
        adapter?.enablePageTracking(enable = false)
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    override fun onResume() {
        super.onResume()
        adapter?.enablePageTracking(enable = true)
    }

    override fun onPodcastClicked(podcast: DiscoverPodcast, listUuid: String?, listDate: String?, isFeatured: Boolean) {
        val fragment = PodcastFragment.newInstance(
            podcastUuid = podcast.uuid,
            fromListUuid = listUuid,
            fromListDate = listDate,
            sourceView = SourceView.DISCOVER,
            featuredPodcast = isFeatured,
        )
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onPodcastSubscribe(podcast: DiscoverPodcast, listUuid: String?, listDate: String?) {
        viewModel.subscribeToPodcast(podcast)
        analyticsTracker.track(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            mapOf(SOURCE_KEY to SourceView.DISCOVER.analyticsValue, UUID_KEY to podcast.uuid),
        )
    }

    override fun onPodcastListClicked(contentList: NetworkLoadableList, podcastList: PodcastList?) {
        val transformedList = viewModel.transformNetworkLoadableList(contentList, resources) // Replace any [regionCode] etc references
        val listId = contentList.listUuid
        val listDate = podcastList?.date ?: ""
        if (listId != null) {
            analyticsTracker.discoverListShowAllTapped(listId = listId, listDate = listDate)
        } else {
            analyticsTracker.discoverShowAllTapped(listId = transformedList.inferredId(), listDate = listDate)
        }
        if (contentList is DiscoverCategory) {
            trackCategoryShownImpression(contentList)
        }

        if (contentList.expandedStyle is ExpandedStyle.GridList) {
            val fragment = PodcastGridFragment.newInstance(transformedList)
            (activity as FragmentHostListener).addFragment(fragment)
        } else {
            val fragment = PodcastListFragment.newInstance(transformedList)
            (activity as FragmentHostListener).addFragment(fragment)
        }
    }

    override fun onCollectionHeaderClicked(list: NetworkLoadableList) {
        val transformedList = viewModel.transformNetworkLoadableList(list, resources)
        val listId = list.listUuid
        if (listId != null) {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_COLLECTION_HEADER_TAPPED, mapOf(LIST_ID_KEY to listId))
        }
        if (list.expandedStyle is ExpandedStyle.GridList) {
            val fragment = PodcastGridFragment.newInstance(transformedList)
            (activity as FragmentHostListener).addFragment(fragment)
        }
    }

    override fun onEpisodeClicked(episode: DiscoverEpisode, listUuid: String?) {
        val fragment = EpisodeContainerFragment.newInstance(
            episodeUuid = episode.uuid,
            source = EpisodeViewSource.DISCOVER,
            podcastUuid = episode.podcast_uuid,
            fromListUuid = listUuid,
        )
        fragment.show(parentFragmentManager, "episode_card")
    }

    override fun onEpisodePlayClicked(episode: DiscoverEpisode) {
        viewModel.findOrDownloadEpisode(episode) { databaseEpisode ->
            viewModel.playEpisode(databaseEpisode)
        }
    }

    override fun onEpisodeStopClicked() {
        viewModel.stopPlayback()
    }

    @Inject lateinit var categoriesManager: CategoriesManager

    override fun onClickSearch() {
        val searchFragment = SearchFragment.newInstance(
            floating = true,
            onlySearchRemote = true,
            source = SourceView.DISCOVER,
        )
        (activity as FragmentHostListener).addFragment(searchFragment, onTop = true)
        binding?.recyclerView?.smoothScrollToPosition(0)
    }

    override fun onSelectCategory(category: DiscoverCategory) {
        analyticsTracker.track(
            AnalyticsEvent.DISCOVER_CATEGORIES_PILL_TAPPED,
            mapOf(
                "name" to category.name,
                "id" to category.id,
                "region" to viewModel.currentRegionCode.orEmpty(),
                "index" to (category.featuredIndex ?: -1),
                "sponsored" to (category.isSponsored == true),
                "visits" to category.totalVisits,
            ),
        )

        categoriesManager.selectCategory(category.id)
    }

    override fun onDismissSelectedCategory(category: DiscoverCategory) {
        analyticsTracker.track(
            AnalyticsEvent.DISCOVER_CATEGORY_CLOSE_BUTTON_TAPPED,
            mapOf("name" to category.name, "id" to category.id, "region" to viewModel.currentRegionCode.orEmpty()),
        )

        categoriesManager.dismissSelectedCategory()
    }

    override fun onShowAllCategories() {
        CategoriesBottomSheet
            .newInstance(viewModel.currentRegionCode.orEmpty())
            .show(childFragmentManager, "categories_bottom_sheet")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        viewModel.onShown()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding?.recyclerView ?: return
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        if (adapter == null) {
            adapter = DiscoverAdapter(
                context = requireContext(),
                staticServiceManager = staticServiceManager,
                listener = this,
                theme = theme,
                loadPodcastList = { source, authenticated -> viewModel.loadPodcastList(source, authenticated) },
                loadCarouselSponsoredPodcastList = viewModel::loadCarouselSponsoredPodcasts,
                categoriesState = { (url, popularIds, sponsoredIds) ->
                    categoriesManager.setRowInfo(popularCategoryIds = popularIds, sponsoredCategoryIds = sponsoredIds)
                    categoriesManager.loadCategories(url)
                    categoriesManager.state.asFlowable()
                },
                analyticsTracker = analyticsTracker,
            )
        }
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        viewLifecycleOwner.lifecycleScope.launch {
            val binding = binding ?: return@launch

            var displayedCategoryId: Int? = null
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.isError) {
                        binding.loading.isVisible = false
                        binding.errorLayout.isVisible = true
                        binding.recyclerView.isVisible = false

                        binding.btnRetry.setOnClickListener { viewModel.loadFeed(resources) }
                        return@collect
                    }

                    binding.loading.isVisible = state.isLoading
                    binding.errorLayout.isVisible = false
                    binding.recyclerView.isVisible = true

                    if (state.rows != null) {
                        adapter?.submitList(state.rows)
                        val newCategoryId = state.categoryFeed?.category?.id
                        if (displayedCategoryId != newCategoryId) {
                            displayedCategoryId = newCategoryId
                            if (newCategoryId != null) {
                                trackCategoryShownImpression(state.categoryFeed.category)
                            }
                        }
                    }

                    val feed = state.discoverFeed ?: return@collect
                    adapter?.onChangeRegion = {
                        val fragment = RegionSelectFragment.newInstance(feed.regionList, feed.selectedRegion)
                        (activity as FragmentHostListener).addFragment(fragment)
                        fragment.listener = this@DiscoverFragment
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding?.recyclerView?.updatePadding(bottom = it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                categoriesManager.selectedCategory.collect { category ->
                    if (category == null) {
                        viewModel.loadFeed(resources)
                    } else {
                        viewModel.loadCategory(category, resources)
                    }
                }
            }
        }
    }

    override fun onRegionSelected(region: DiscoverRegion) {
        viewModel.changeRegion(region, resources)

        @Suppress("DEPRECATION")
        activity?.onBackPressed()

        binding?.recyclerView?.scrollToPosition(0)
    }

    private fun trackCategoryShownImpression(category: DiscoverCategory) {
        viewModel.currentRegionCode?.let {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_CATEGORY_SHOWN,
                mapOf(
                    NAME_KEY to category.name,
                    REGION_KEY to it,
                    ID_KEY to category.id,
                ),
            )
        }
    }

    override fun scrollToTop(): Boolean {
        val canScroll = binding?.recyclerView?.canScrollVertically(-1) ?: false
        binding?.recyclerView?.quickScrollToTop()
        return canScroll
    }

    companion object {
        private const val ID_KEY = "id"
        private const val NAME_KEY = "name"
        private const val REGION_KEY = "region"
        const val LIST_ID_KEY = "list_id"
        const val PODCAST_UUID_KEY = "podcast_uuid"
        const val EPISODE_UUID_KEY = "episode_uuid"
        const val SOURCE_KEY = "source"
        const val UUID_KEY = "uuid"
    }
}
