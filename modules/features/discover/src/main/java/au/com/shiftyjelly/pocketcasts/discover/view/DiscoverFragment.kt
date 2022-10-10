package au.com.shiftyjelly.pocketcasts.discover.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.databinding.FragmentDiscoverBinding
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverState
import au.com.shiftyjelly.pocketcasts.discover.viewmodel.DiscoverViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DiscoverFragment : BaseFragment(), DiscoverAdapter.Listener, RegionSelectFragment.Listener {
    override var statusBarColor: StatusBarColor = StatusBarColor.Dark

    @Inject lateinit var settings: Settings
    @Inject lateinit var staticServerManager: StaticServerManagerImpl
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: DiscoverViewModel by viewModels()
    private var adapter: DiscoverAdapter? = null
    private var binding: FragmentDiscoverBinding? = null

    override fun onPodcastClicked(podcast: DiscoverPodcast, listUuid: String?) {
        val fragment = PodcastFragment.newInstance(podcastUuid = podcast.uuid, fromListUuid = listUuid)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onPodcastSubscribe(podcast: DiscoverPodcast, listUuid: String?) {
        viewModel.subscribeToPodcast(podcast)
    }

    override fun onPodcastListClicked(list: NetworkLoadableList) {
        val transformedList = viewModel.transformNetworkLoadableList(list, resources) // Replace any [regionCode] etc references
        val listId = list.listUuid
        if (listId != null) {
            FirebaseAnalyticsTracker.listShowAllTapped(listId)
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED,
                mapOf(AnalyticsPropKey.LIST_ID to AnalyticsPropValue(listId))
            )
        } else {
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_SHOW_ALL_TAPPED,
                mapOf(AnalyticsPropKey.LIST_ID to AnalyticsPropValue(transformedList.inferredId()))
            )
        }
        if (list is DiscoverCategory) {
            viewModel.currentRegionCode?.let {
                FirebaseAnalyticsTracker.openedCategory(list.id, it)
                analyticsTracker.track(
                    AnalyticsEvent.DISCOVER_CATEGORY_SHOWN,
                    mapOf(
                        AnalyticsPropKey.NAME to AnalyticsPropValue(list.name),
                        AnalyticsPropKey.REGION to AnalyticsPropValue(it),
                        AnalyticsPropKey.ID to AnalyticsPropValue(list.id)
                    )
                )
            }
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
        val fragment = EpisodeFragment.newInstance(episodeUuid = episode.uuid, podcastUuid = episode.podcast_uuid, fromListUuid = listUuid)
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
        val searchFragment = SearchFragment.newInstance(floating = true, onlySearchRemote = true)
        (activity as FragmentHostListener).addFragment(searchFragment, onTop = true)
        binding?.recyclerView?.smoothScrollToPosition(0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false)

        if (viewModel.state.value !is DiscoverState.DataLoaded) {
            viewModel.loadData(resources)
        }

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
            adapter = DiscoverAdapter(viewModel.repository, staticServerManager, this, theme, viewModel::loadPodcastList, analyticsTracker)
        }
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state ->
                val binding = binding ?: return@Observer
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
                        adapter?.submitList(content)
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
                }
            }
        )
    }

    override fun onRegionSelected(region: DiscoverRegion) {
        viewModel.changeRegion(region, resources)

        @Suppress("DEPRECATION")
        activity?.onBackPressed()

        binding?.recyclerView?.scrollToPosition(0)
    }

    override fun setUserVisibleHint(visible: Boolean) {
        super.setUserVisibleHint(visible)
        if (visible) {
            FirebaseAnalyticsTracker.navigatedToDiscover()
        }
    }

    companion object {
        object AnalyticsPropKey {
            const val ID = "id"
            const val NAME = "name"
            const val REGION = "region"
            const val LIST_ID = "list_id"
            const val PODCAST_UUID = "podcast_uuid"
            const val EPISODE_UUID = "episode_uuid"
        }
    }
}
