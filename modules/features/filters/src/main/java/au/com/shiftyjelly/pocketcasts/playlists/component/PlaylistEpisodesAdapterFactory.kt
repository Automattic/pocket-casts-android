package au.com.shiftyjelly.pocketcasts.playlists.component

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.playlists.manual.UnavailableEpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import dagger.hilt.android.lifecycle.withCreationCallback
import dagger.hilt.android.scopes.FragmentScoped
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch

@FragmentScoped
class PlaylistEpisodesAdapterFactory @Inject constructor(
    private val fragment: Fragment,
    private val rowDataProvider: EpisodeRowDataProvider,
    private val settings: Settings,
    private val playButtonListener: PlayButton.OnClickListener,
    private val multiSelectHelper: MultiSelectEpisodesHelper,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
) : HasBackstack {
    override fun onBackPressed(): Boolean {
        return if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
            true
        } else {
            false
        }
    }

    override fun getBackstackCount(): Int {
        return if (multiSelectHelper.isMultiSelecting) 1 else 0
    }

    fun create(
        playlistType: Playlist.Type,
        playlistUuid: String,
        multiSelectToolbar: MultiSelectToolbar,
        getEpisodes: () -> List<PlaylistEpisode>,
    ): PlaylistEpisodeAdapter {
        lateinit var adapter: PlaylistEpisodeAdapter
        configureDependencies(
            getAdapter = { adapter },
            multiSelectToolbar = multiSelectToolbar,
            getEpisodes = { getEpisodes().map(PlaylistEpisode::toMultiselectEpisode) },
        )
        val parentFragmentManager = fragment.parentFragmentManager
        val childFragmentManager = fragment.childFragmentManager

        val swipeActionViewModel by fragment.viewModels<SwipeActionViewModel>(
            extrasProducer = {
                fragment.defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                    factory.create(SwipeSource.Filters, playlistUuid)
                }
            },
        )

        adapter = PlaylistEpisodeAdapter(
            playlistType = playlistType,
            rowDataProvider = rowDataProvider,
            settings = settings,
            onRowClick = { episodeWrapper ->
                when (episodeWrapper) {
                    is PlaylistEpisode.Available -> if (parentFragmentManager.findFragmentByTag("episode_card") == null) {
                        EpisodeContainerFragment.newInstance(
                            episode = episodeWrapper.episode,
                            source = EpisodeViewSource.FILTERS,
                        ).show(parentFragmentManager, "episode_card")
                    }

                    is PlaylistEpisode.Unavailable -> if (childFragmentManager.findFragmentByTag("unavailable_episode_sheet") == null) {
                        UnavailableEpisodeFragment.newInstance(
                            episodeUuid = episodeWrapper.uuid,
                        ).show(childFragmentManager, "unavailable_episode_sheet")
                    }
                }
            },
            onSwipeAction = { episode, action ->
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(action, episode.uuid, childFragmentManager)
                }
            },
            playButtonListener = playButtonListener,
            imageRequestFactory = PocketCastsImageRequestFactory(fragment.requireContext()).themed().smallSize(),
            multiSelectHelper = multiSelectHelper,
            swipeRowActionsFactory = swipeRowActionsFactory,
            fragmentManager = childFragmentManager,
        )

        return adapter
    }

    fun startMultiSelecting() {
        multiSelectHelper.isMultiSelecting = true
    }

    private fun configureDependencies(
        getAdapter: () -> RecyclerView.Adapter<*>,
        multiSelectToolbar: MultiSelectToolbar,
        getEpisodes: () -> List<BaseEpisode>,
    ) {
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                multiSelectHelper.isMultiSelecting = false
            }

            override fun onDestroy(owner: LifecycleOwner) {
                multiSelectHelper.cleanup()
            }
        })

        playButtonListener.source = SourceView.FILTERS

        multiSelectHelper.apply {
            context = fragment.requireActivity()
            source = SourceView.FILTERS

            var isLoaded = false
            isMultiSelectingLive.observe(fragment.viewLifecycleOwner) { isMultiSelecting ->
                if (!isLoaded) {
                    isLoaded = true
                    return@observe
                }

                val wasMultiSelecting = multiSelectToolbar.isVisible
                if (wasMultiSelecting == isMultiSelecting) {
                    return@observe
                }
                multiSelectToolbar.isVisible = isMultiSelecting

                val event = if (isMultiSelecting) {
                    AnalyticsEvent.FILTER_MULTI_SELECT_ENTERED
                } else {
                    AnalyticsEvent.FILTER_MULTI_SELECT_EXITED
                }
                analyticsTracker.track(event)
                getAdapter().notifyItemRangeChanged(0, getEpisodes().size, MULTI_SELECT_TOGGLE_PAYLOAD)
            }
            listener = object : MultiSelectHelper.Listener<BaseEpisode> {
                override fun multiSelectSelectAll() {
                    analyticsTracker.track(AnalyticsEvent.FILTER_SELECT_ALL)
                    val episodes = getEpisodes()
                    multiSelectHelper.selectAllInList(episodes)
                    @SuppressLint("NotifyDataSetChanged")
                    getAdapter().notifyDataSetChanged()
                }

                override fun multiSelectSelectNone() {
                    analyticsTracker.track(AnalyticsEvent.FILTER_DESELECT_ALL)
                    val episodes = getEpisodes()
                    multiSelectHelper.deselectAllInList(episodes)
                    @SuppressLint("NotifyDataSetChanged")
                    getAdapter().notifyDataSetChanged()
                }

                override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
                    analyticsTracker.track(AnalyticsEvent.FILTER_SELECT_ALL_ABOVE)
                    val episodes = getEpisodes()
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
                        getAdapter().notifyItemRangeChanged(0, startIndex + 1)
                    }
                }

                override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
                    analyticsTracker.track(AnalyticsEvent.FILTER_DESELECT_ALL_BELOW)
                    val episodes = getEpisodes()
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesBelow = episodes.subList(startIndex, episodes.size)
                        multiSelectHelper.deselectAllInList(episodesBelow)
                        getAdapter().notifyItemRangeChanged(startIndex, episodes.size)
                    }
                }

                override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
                    analyticsTracker.track(AnalyticsEvent.FILTER_DESELECT_ALL_ABOVE)
                    val episodes = getEpisodes()
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesAbove = episodes.subList(0, startIndex + 1)
                        multiSelectHelper.deselectAllInList(episodesAbove)
                        getAdapter().notifyItemRangeChanged(0, startIndex + 1)
                    }
                }

                override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
                    analyticsTracker.track(AnalyticsEvent.FILTER_SELECT_ALL_BELOW)
                    val episodes = getEpisodes()
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
                        getAdapter().notifyItemRangeChanged(startIndex, episodes.size)
                    }
                }
            }
            multiSelectToolbar.setup(fragment.viewLifecycleOwner, multiSelectHelper, menuRes = null, activity = fragment.requireActivity())
        }
    }
}

private fun PlaylistEpisode.toMultiselectEpisode() = when (this) {
    is PlaylistEpisode.Available -> episode
    is PlaylistEpisode.Unavailable -> unavailableMultiselectPlaceholder
}

private val unavailableMultiselectPlaceholder = PodcastEpisode(uuid = "unavailable", publishedDate = Date(0))
