package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeListAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutViewModel
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class PlaylistEpisodesAdapterFactory @Inject constructor(
    private val fragment: Fragment,
    private val bookmarkManager: BookmarkManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val settings: Settings,
    private val playButtonListener: PlayButton.OnClickListener,
    private val multiSelectHelper: MultiSelectEpisodesHelper,
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
        multiSelectToolbar: MultiSelectToolbar,
        onChangeMultiSelect: (Boolean) -> Unit,
        getEpisodes: () -> List<BaseEpisode>,
    ): EpisodeListAdapter {
        lateinit var adapter: EpisodeListAdapter
        configureDependencies(
            getAdapter = { adapter },
            multiSelectToolbar = multiSelectToolbar,
            onChangeMultiSelect = onChangeMultiSelect,
            getEpisodes = getEpisodes,
        )
        val parentFragmentManager = fragment.parentFragmentManager
        val childFragmentManager = fragment.childFragmentManager
        val swipeButtonViewModel by fragment.viewModels<SwipeButtonLayoutViewModel>()

        adapter = EpisodeListAdapter(
            bookmarkManager = bookmarkManager,
            downloadManager = downloadManager,
            playbackManager = playbackManager,
            upNextQueue = upNextQueue,
            settings = settings,
            onRowClick = { episode ->
                when (episode) {
                    is PodcastEpisode -> if (parentFragmentManager.findFragmentByTag("episode_card") == null) {
                        EpisodeContainerFragment.newInstance(episode, EpisodeViewSource.FILTERS).show(parentFragmentManager, "episode_card")
                    }

                    is UserEpisode -> Unit
                }
            },
            playButtonListener = playButtonListener,
            imageRequestFactory = PocketCastsImageRequestFactory(fragment.requireContext()).themed().smallSize(),
            multiSelectHelper = multiSelectHelper,
            fragmentManager = childFragmentManager,
            swipeButtonLayoutFactory = SwipeButtonLayoutFactory(
                swipeButtonLayoutViewModel = swipeButtonViewModel,
                onItemUpdated = { _, index -> adapter.notifyItemChanged(index) },
                defaultUpNextSwipeAction = { settings.upNextSwipe.value },
                fragmentManager = parentFragmentManager,
                swipeSource = EpisodeItemTouchHelper.SwipeSource.FILTERS,
            ),
            artworkContext = Element.Filters,
        )

        return adapter
    }

    private fun configureDependencies(
        getAdapter: () -> EpisodeListAdapter,
        multiSelectToolbar: MultiSelectToolbar,
        onChangeMultiSelect: (Boolean) -> Unit,
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
                onChangeMultiSelect(isMultiSelecting)
                @SuppressLint("NotifyDataSetChanged")
                getAdapter().notifyDataSetChanged()
            }
            listener = object : MultiSelectHelper.Listener<BaseEpisode> {
                override fun multiSelectSelectAll() {
                    analyticsTracker.track(AnalyticsEvent.FILTER_SELECT_ALL_BUTTON_TAPPED)
                    val episodes = getEpisodes()
                    multiSelectHelper.selectAllInList(episodes)
                    @SuppressLint("NotifyDataSetChanged")
                    getAdapter().notifyDataSetChanged()
                }

                override fun multiSelectSelectNone() {
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
