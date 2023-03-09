package au.com.shiftyjelly.pocketcasts.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_ONLY_SEARCH_REMOTE = "arg_only_search_remote"
private const val ARG_SOURCE = "arg_source"
private const val ARG_TYPE = "arg_type"
@AndroidEntryPoint
class SearchResultsFragment : BaseFragment() {
    private val viewModel by viewModels<SearchViewModel>({ requireParentFragment() })
    private val searchHistoryViewModel by viewModels<SearchHistoryViewModel>()
    private var listener: SearchFragment.Listener? = null

    private val onlySearchRemote: Boolean
        get() = arguments?.getBoolean(ARG_ONLY_SEARCH_REMOTE) ?: false
    private val source: AnalyticsSource
        get() = AnalyticsSource.fromString(arguments?.getString(ARG_SOURCE))
    private val type: ResultsType
        get() = ResultsType.fromString(arguments?.getString(ARG_TYPE))

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as SearchFragment.Listener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                when (type) {
                    ResultsType.PODCASTS -> {
                        SearchPodcastResultsPage(
                            viewModel = viewModel,
                            onFolderClick = ::onFolderClick,
                            onPodcastClick = ::onPodcastClick,
                            onBackClick = ::onBackClick,
                        )
                    }

                    ResultsType.EPISODES -> {
                        SearchEpisodeResultsPage(
                            viewModel = viewModel,
                            onBackClick = ::onBackClick,
                            onEpisodeClick = ::onEpisodeClick
                        )
                    }

                    ResultsType.UNKNOWN -> throw IllegalStateException("Unknown search results type")
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            UiUtil.hideKeyboard(view)
        }
    }

    private fun onEpisodeClick(episodeItem: EpisodeItem) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = episodeItem.uuid,
            type = SearchViewModel.SearchResultType.EPISODE,
        )
        val episode = episodeItem.toEpisode()
        searchHistoryViewModel.add(SearchHistoryEntry.fromEpisode(episode, episodeItem.podcastTitle))
        listener?.onSearchEpisodeClick(
            episodeUuid = episode.uuid,
            podcastUuid = episode.podcastUuid,
            source = EpisodeViewSource.SEARCH
        )
    }

    private fun onFolderClick(folder: Folder, podcasts: List<Podcast>) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = folder.uuid,
            type = SearchViewModel.SearchResultType.FOLDER,
        )
        searchHistoryViewModel.add(SearchHistoryEntry.fromFolder(folder, podcasts.map { it.uuid }))
        listener?.onSearchFolderClick(folder.uuid)
    }

    private fun onPodcastClick(podcast: Podcast) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = podcast.uuid,
            type = if (onlySearchRemote || !podcast.isSubscribed) {
                SearchViewModel.SearchResultType.PODCAST_REMOTE_RESULT
            } else {
                SearchViewModel.SearchResultType.PODCAST_LOCAL_RESULT
            }
        )
        searchHistoryViewModel.add(SearchHistoryEntry.fromPodcast(podcast))
        listener?.onSearchPodcastClick(podcast.uuid)
    }

    private fun onBackClick() {
        parentFragmentManager.popBackStack()
    }

    companion object {
        enum class ResultsType(val value: String) {
            PODCASTS("podcasts"),
            EPISODES("episodes"),
            UNKNOWN("unknown");

            companion object {
                fun fromString(value: String?) =
                    ResultsType.values().find { it.value == value } ?: UNKNOWN
            }
        }

        fun newInstance(
            type: ResultsType,
            onlySearchRemote: Boolean = false,
            source: AnalyticsSource
        ): SearchResultsFragment {
            val fragment = SearchResultsFragment()
            val arguments = Bundle().apply {
                putString(ARG_TYPE, type.value)
                putBoolean(ARG_ONLY_SEARCH_REMOTE, onlySearchRemote)
                putString(ARG_SOURCE, source.analyticsValue)
            }
            fragment.arguments = arguments
            return fragment
        }
    }
}
