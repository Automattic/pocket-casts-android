package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.FragmentDiscoverBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverState
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverViewModel
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ProfileButton
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class DiscoverFragment : BaseFragment(), DiscoverAdapter.Listener, RegionSelectFragment.Listener {
    override var statusBarColor: StatusBarColor = StatusBarColor.Dark

    @Inject lateinit var settings: Settings

    @Inject lateinit var staticServerManager: StaticServerManagerImpl

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: DiscoverViewModel by viewModels()
    private var adapter: DiscoverAdapter? = null
    private var binding: FragmentDiscoverBinding? = null

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    override fun onPodcastClicked(podcast: DiscoverPodcast, listUuid: String?) {
        val fragment = PodcastFragment.newInstance(podcastUuid = podcast.uuid, fromListUuid = listUuid, sourceView = SourceView.DISCOVER)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onPodcastSubscribe(podcast: DiscoverPodcast, listUuid: String?) {
        viewModel.subscribeToPodcast(podcast)
        analyticsTracker.track(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            mapOf(SOURCE_KEY to SourceView.DISCOVER.analyticsValue, UUID_KEY to podcast.uuid),
        )
    }

    override fun onPodcastListClicked(list: NetworkLoadableList) {
        val transformedList = viewModel.transformNetworkLoadableList(list, resources) // Replace any [regionCode] etc references
        val listId = list.listUuid
        if (listId != null) {
            FirebaseAnalyticsTracker.listShowAllTapped(listId)
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED, mapOf(LIST_ID_KEY to listId))
        } else {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_SHOW_ALL_TAPPED, mapOf(LIST_ID_KEY to transformedList.inferredId()))
        }
        if (list is DiscoverCategory) {
            trackCategoryImpression(list)
        }

        if (list.expandedStyle is ExpandedStyle.GridList) {
            val fragment = PodcastGridFragment.newInstance(transformedList)
            (activity as FragmentHostListener).addFragment(fragment)
        } else {
            val fragment = PodcastListFragment.newInstance(transformedList)
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

    override fun onSearchClicked() {
        val searchFragment = SearchFragment.newInstance(
            floating = true,
            onlySearchRemote = true,
            source = SourceView.DISCOVER,
        )
        (activity as FragmentHostListener).addFragment(searchFragment, onTop = true)
        binding?.recyclerView?.smoothScrollToPosition(0)
    }

    override fun onCategoryClick(selectedCategory: CategoryPill, onCategorySelectionSuccess: () -> Unit) {
        trackCategoryImpression(selectedCategory.discoverCategory)

        val categoryWithRegionUpdated =
            viewModel.transformNetworkLoadableList(selectedCategory.discoverCategory, resources)

        viewModel.filterPodcasts(categoryWithRegionUpdated.source, categoryWithRegionUpdated.source) {
            val podcasts = it.podcasts

            val mostPopularPodcasts =
                MostPopularPodcastsByCategoryRow(it.listId, it.title, podcasts.take(MOST_POPULAR_PODCASTS))

            val remainingPodcasts =
                RemainingPodcastsByCategoryRow(it.listId, it.title, podcasts.drop(MOST_POPULAR_PODCASTS))

            updateDiscoverWithCategorySelected(selectedCategory.discoverCategory.id, mostPopularPodcasts, remainingPodcasts)

            onCategorySelectionSuccess()
        }
    }
    override fun onAllCategoriesClick(source: String, onCategorySelectionSuccess: (CategoryPill) -> Unit, onCategorySelectionCancel: () -> Unit) {
        viewModel.loadCategories(source) { categories ->
            CategoriesBottomSheet(
                categories = categories,
                onCategoryClick = { this.onCategoryClick(it) { onCategorySelectionSuccess(it) } },
                onCategorySelectionCancel = onCategorySelectionCancel,
            ).show(childFragmentManager, "categories_bottom_sheet")
        }
    }
    override fun onClearCategoryFilterClick(source: String, onCategoryClearSuccess: (List<CategoryPill>) -> Unit) {
        viewModel.loadCategories(source) { categories ->
            onCategoryClearSuccess(categories)
            viewModel.loadData(resources) // Reload discover
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false)

        if (viewModel.state.value !is DiscoverState.DataLoaded) {
            viewModel.loadData(resources)
        }

        viewModel.onShown()

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        if (FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR)) {
            binding.appBarLayout.isVisible = true
            setupToolbarAndStatusBar(
                toolbar = binding.toolbar,
                title = getString(LR.string.discover),
                menu = R.menu.discover_menu,
                navigationIcon = NavigationIcon.None,
                profileButton = ProfileButton.Shown(),
            )
            binding.toolbar.menu.findItem(UR.id.menu_profile).isVisible = true
        } else {
            binding.recyclerView.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = 0 }
        }

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        if (adapter == null) {
            adapter = DiscoverAdapter(
                context = requireContext(),
                service = viewModel.repository,
                staticServerManager = staticServerManager,
                listener = this,
                theme = theme,
                loadPodcastList = viewModel::loadPodcastList,
                loadCarouselSponsoredPodcastList = viewModel::loadCarouselSponsoredPodcasts,
                loadCategories = viewModel::loadCategories,
                analyticsTracker = analyticsTracker,
            )
        }
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is DiscoverState.DataLoaded -> {
                        binding.errorLayout.isVisible = false
                        binding.recyclerView.isVisible = true
                        binding.loading.isVisible = false

                        val content = state.data.plus(ChangeRegionRow(state.selectedRegion))
                        val onChangeRegion: () -> Unit = {
                            val fragment = RegionSelectFragment.newInstance(state.regionList, state.selectedRegion)
                            (activity as FragmentHostListener).addFragment(fragment)
                            fragment.listener = this
                        }
                        adapter?.onChangeRegion = onChangeRegion

                        val updatedContent = updateDiscoverRowsAndRemoveCategoryAds(content, state.selectedRegion.code)

                        adapter?.submitList(updatedContent)
                    }
                    is DiscoverState.Error -> {
                        binding.errorLayout.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.loading.isVisible = false

                        binding.btnRetry.setOnClickListener { viewModel.loadData(resources) }
                    }
                    is DiscoverState.Loading -> {
                        binding.errorLayout.isVisible = false
                        binding.recyclerView.isVisible = false
                        binding.loading.isVisible = true
                    }
                    is DiscoverState.FilteringPodcastsByCategory -> {
                        binding.loading.isVisible = true
                        binding.errorLayout.isVisible = false
                    }
                    is DiscoverState.PodcastsFilteredByCategory -> {
                        binding.errorLayout.isVisible = false
                        binding.recyclerView.isVisible = true
                        binding.loading.isVisible = false
                    }
                }
            },
        )
    }

    override fun onRegionSelected(region: DiscoverRegion) {
        viewModel.changeRegion(region, resources)

        @Suppress("DEPRECATION")
        activity?.onBackPressed()

        binding?.recyclerView?.scrollToPosition(0)
    }

    @Suppress("DEPRECATION")
    override fun setUserVisibleHint(visible: Boolean) {
        super.setUserVisibleHint(visible)
        if (visible) {
            FirebaseAnalyticsTracker.navigatedToDiscover()
        }
    }
    private fun updateDiscoverRowsAndRemoveCategoryAds(content: List<Any>, region: String): MutableList<Any> {
        val mutableContentList = content.toMutableList()

        val categoriesIndex = mutableContentList.indexOfFirst { it is DiscoverRow && it.type is ListType.Categories }

        if (categoriesIndex != -1) {
            val categoriesItem = mutableContentList[categoriesIndex] as DiscoverRow
            mutableContentList[categoriesIndex] = categoriesItem.copy(regionCode = region)
        }

        mutableContentList.removeAll { it is DiscoverRow && it.categoryId != null } // Remove ads exclusive to category view

        return mutableContentList
    }

    private fun updateDiscoverWithCategorySelected(
        categoryId: Int,
        mostPopularPodcasts: MostPopularPodcastsByCategoryRow,
        remainingPodcasts: RemainingPodcastsByCategoryRow,
    ) {
        adapter?.currentList?.let { discoverList ->
            val updatedList = discoverList.filter { it is DiscoverRow && it.type is ListType.Categories }.toMutableList()

            // First, we insert the most popular podcasts.
            updatedList.add(mostPopularPodcasts)

            // If there is ad, we add it.
            viewModel.getAdForCategoryView(categoryId)?.let { updatedList.add(CategoryAdRow(it)) }

            // Lastly, we add the remaining podcast list.
            updatedList.add(remainingPodcasts)

            adapter?.submitList(updatedList)
        }
    }
    private fun trackCategoryImpression(category: DiscoverCategory) {
        viewModel.currentRegionCode?.let {
            FirebaseAnalyticsTracker.openedCategory(category.id, it)
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
    companion object {
        private const val ID_KEY = "id"
        private const val NAME_KEY = "name"
        private const val REGION_KEY = "region"
        private const val MOST_POPULAR_PODCASTS = 5
        const val LIST_ID_KEY = "list_id"
        const val CATEGORY_ID_KEY = "category_id"
        const val PODCAST_UUID_KEY = "podcast_uuid"
        const val EPISODE_UUID_KEY = "episode_uuid"
        const val SOURCE_KEY = "source"
        const val UUID_KEY = "uuid"
    }
}
