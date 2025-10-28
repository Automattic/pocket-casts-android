package au.com.shiftyjelly.pocketcasts.search

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.search.SearchResultsFragment.Companion.ResultsType
import au.com.shiftyjelly.pocketcasts.search.SearchViewModel.SearchResultType
import au.com.shiftyjelly.pocketcasts.search.databinding.FragmentSearchBinding
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryClearAllConfirmationDialog
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryPage
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.extensions.showKeyboard
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ARG_FLOATING = "arg_floating"
private const val ARG_ONLY_SEARCH_REMOTE = "arg_only_search_remote"
private const val ARG_SOURCE = "arg_source"
private const val SEARCH_HISTORY_CLEAR_ALL_CONFIRMATION_DIALOG_TAG = "search_history_clear_all_confirmation_dialog"
private const val SEARCH_RESULTS_TAG = "search_results"

@AndroidEntryPoint
class SearchFragment : BaseFragment() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var playButtonListener: PlayButtonListener

    interface Listener {
        fun onSearchEpisodeClick(episodeUuid: String, podcastUuid: String, source: EpisodeViewSource)
        fun onSearchPodcastClick(podcastUuid: String, source: SourceView)
        fun onSearchFolderClick(folderUuid: String)
    }

    companion object {
        fun newInstance(
            floating: Boolean = false,
            onlySearchRemote: Boolean = false,
            source: SourceView,
        ): SearchFragment {
            val fragment = SearchFragment()
            val arguments = Bundle().apply {
                putBoolean(ARG_FLOATING, floating)
                putBoolean(ARG_ONLY_SEARCH_REMOTE, onlySearchRemote)
                putString(ARG_SOURCE, source.analyticsValue)
            }
            fragment.arguments = arguments
            return fragment
        }
    }

    private val viewModel: SearchViewModel by viewModels()
    private val searchHistoryViewModel: SearchHistoryViewModel by viewModels()
    private var listener: Listener? = null
    private var binding: FragmentSearchBinding? = null

    val floating: Boolean
        get() = arguments?.getBoolean(ARG_FLOATING) ?: false
    val onlySearchRemote: Boolean
        get() = arguments?.getBoolean(ARG_ONLY_SEARCH_REMOTE) ?: false
    val source: SourceView
        get() = SourceView.fromString(arguments?.getString(ARG_SOURCE))

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as Listener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.trackSearchShownOrDismissed(AnalyticsEvent.SEARCH_SHOWN, source)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)
        viewModel.setOnlySearchRemote(onlySearchRemote)
        searchHistoryViewModel.setOnlySearchRemote(onlySearchRemote)
        searchHistoryViewModel.setSource(source)
        binding.floatingLayout.updatePadding(
            top = if (floating) {
                binding.floatingLayout.context.resources.getDimensionPixelSize(
                    R.dimen.search_box_floating_top,
                )
            } else {
                0
            },
        )

        this.binding = binding

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        binding?.let {
            UiUtil.hideKeyboard(it.searchView)
            it.searchView.clearFocus()
        }
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun navigateFromSearchHistoryEntry(entry: SearchHistoryEntry) {
        searchHistoryViewModel.trackEventForEntry(AnalyticsEvent.SEARCH_HISTORY_ITEM_TAPPED, entry)
        when (entry) {
            is SearchHistoryEntry.Episode -> listener?.onSearchEpisodeClick(
                episodeUuid = entry.uuid,
                podcastUuid = entry.podcastUuid,
                source = EpisodeViewSource.SEARCH_HISTORY,
            )

            is SearchHistoryEntry.Folder -> listener?.onSearchFolderClick(entry.uuid)
            is SearchHistoryEntry.Podcast -> listener?.onSearchPodcastClick(entry.uuid, SourceView.SEARCH)
            is SearchHistoryEntry.SearchTerm -> {
                binding?.let {
                    viewModel.runSearchOnTerm(entry.term)
                    it.searchView.setQuery(entry.term, true)
                    it.searchHistoryPanel.hide()
                    UiUtil.hideKeyboard(it.searchView)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        view.setOnClickListener {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }

        binding.backButton.setOnClickListener {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }
        viewModel.setSource(source)

        // hack as Search View ignores text size
        val searchView = binding.searchView
        searchView.findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)?.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(context.getThemeColor(UR.attr.secondary_text_01))
            val hintColor = UR.attr.secondary_text_02
            setHintTextColor(context.getThemeColor(hintColor))
            setOnEditorActionListener { _, actionId, event ->
                if (searchView.query.toString().isNotBlank() && (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER))) {
                    viewModel.runSearchOnTerm(searchView.query.toString())
                    binding.searchHistoryPanel.hide()
                    UiUtil.hideKeyboard(searchView)
                    true
                } else {
                    false
                }
            }
        }

        val searchManager = view.context.getSystemService(Activity.SEARCH_SERVICE) as SearchManager
        activity?.let { searchView.setSearchableInfo(searchManager.getSearchableInfo(it.componentName)) }
        searchView.queryHint = getString(LR.string.search_podcasts_or_add_url)
        searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        searchView.setIconifiedByDefault(false)
        // seems like a more reliable focus using a post
        if (viewModel.state.value.searchTerm.isNullOrBlank()) {
            searchView.post {
                searchView.showKeyboard()
            }
        }
        searchView.setOnCloseListener {
            UiUtil.hideKeyboard(searchView)
            true
        }

        lifecycleScope.launch {
            // detect when we select a suggestion
            viewModel.state
                .scan<SearchUiState, Pair<SearchUiState?, SearchUiState?>>(null to null) { acc, value -> acc.second to value }
                .drop(1)
                .filter { (previous, next) -> previous is SearchUiState.Suggestions && (next is SearchUiState.OldResults || next is SearchUiState.ImprovedResults) }
                .mapNotNull { (_, next) -> next?.searchTerm }
                .collect {
                    if (it != searchView.query) {
                        searchView.setQuery(it, false)
                    }
                }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.updateSearchQuery(query, immediate = true)
                binding.searchHistoryPanel.hide()
                UiUtil.hideKeyboard(searchView)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                // don't auto search for feed URLs
                val characterCount = query.length
                val lowerCaseSearch = query.lowercase()
                if ((characterCount == 1 && lowerCaseSearch.startsWith("h")) || (characterCount == 2 && lowerCaseSearch.startsWith("ht")) || (characterCount == 3 && lowerCaseSearch.startsWith("htt")) || lowerCaseSearch.startsWith("http")) {
                    if (((viewModel.state.value as? SearchUiState.OldResults)?.operation as? SearchUiState.SearchOperation.Success)?.results?.podcasts?.isNotEmpty() == true) {
                        binding.searchHistoryPanel.hide()
                    }
                    return true
                }
                viewModel.updateSearchQuery(query)
                if (characterCount > 0) {
                    binding.searchHistoryPanel.hide()
                } else {
                    binding.searchHistoryPanel.show()
                }
                return true
            }
        })
        binding.searchHistoryPanel.apply {
            setContentWithViewCompositionStrategy {
                val state = viewModel.state.collectAsState()
                if ((state.value is SearchUiState.Suggestions || !FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS)) && state.value.searchTerm.isNullOrBlank()) {
                    searchHistoryViewModel.start()
                }

                AppTheme(theme.activeTheme) {
                    SearchHistoryPage(
                        viewModel = searchHistoryViewModel,
                        onClick = ::navigateFromSearchHistoryEntry,
                        onShowClearAllConfirmation = {
                            SearchHistoryClearAllConfirmationDialog(
                                context = this@SearchFragment.requireContext(),
                                onConfirm = { searchHistoryViewModel.clearAll() },
                            ).show(parentFragmentManager, SEARCH_HISTORY_CLEAR_ALL_CONFIRMATION_DIALOG_TAG)
                        },
                        onScroll = { UiUtil.hideKeyboard(searchView) },
                    )
                    if (viewModel.isFragmentChangingConfigurations && viewModel.showSearchHistory) {
                        binding.searchHistoryPanel.show()
                    }
                }
            }
        }

        binding.searchSuggestions.apply {
            setContentWithViewCompositionStrategy {
                val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
                val state by viewModel.state.collectAsState()
                (state as? SearchUiState.Suggestions)?.let { suggestions ->
                    AppThemeWithBackground(theme.activeTheme) {
                        SearchAutoCompleteResultsPage(
                            modifier = Modifier
                                .fillMaxSize(),
                            searchTerm = suggestions.operation.searchTerm,
                            results = (suggestions.operation as? SearchUiState.SearchOperation.Success)?.results ?: emptyList(),
                            onTermClick = { viewModel.selectSuggestion(it.term) },
                            onPodcastClick = { onPodcastClick(SearchHistoryEntry.fromAutoCompletePodcast(it), it.isSubscribed) },
                            onPodcastFollow = { viewModel.onSubscribeToPodcast(it.uuid) },
                            onFolderClick = { onFolderClick(SearchHistoryEntry.fromAutoCompleteFolder(it)) },
                            onEpisodeClick = {},
                            playButtonListener = playButtonListener,
                            onScroll = { UiUtil.hideKeyboard(searchView) },
                            bottomInset = bottomInset.pxToDp(LocalContext.current).dp,
                            isLoading = suggestions.operation is SearchUiState.SearchOperation.Loading,
                            onReportSuggestionsRender = viewModel::trackSuggestionsShown,
                        )
                    }
                }
                binding.searchSuggestions.isVisible = state is SearchUiState.Suggestions
            }
        }

        binding.searchInlineResults.apply {
            setContentWithViewCompositionStrategy {
                val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
                val state by viewModel.state.collectAsState()
                AppThemeWithBackground(theme.activeTheme) {
                    when (val state = state) {
                        is SearchUiState.ImprovedResults ->
                            ImprovedSearchResultsPage(
                                state = state,
                                loading = state.isLoading,
                                onEpisodeClick = { onEpisodeClick(episode = SearchHistoryEntry.fromImprovedEpisodeResult(it)) },
                                onPodcastClick = { onPodcastClick(SearchHistoryEntry.fromImprovedPodcastResult(it), it.isFollowed) },
                                onFolderClick = ::onFolderClick,
                                onFollowPodcast = { viewModel.onSubscribeToPodcast(it.uuid) },
                                playButtonListener = playButtonListener,
                                onScroll = { UiUtil.hideKeyboard(searchView) },
                                bottomInset = bottomInset.pxToDp(LocalContext.current).dp,
                                onFilterSelect = viewModel::selectFilter,
                            )

                        is SearchUiState.OldResults ->
                            SearchInlineResultsPage(
                                state = state,
                                loading = state.isLoading,
                                onEpisodeClick = ::onEpisodeClick,
                                onPodcastClick = { onPodcastClick(SearchHistoryEntry.fromPodcast(it), it.isSubscribed) },
                                onFolderClick = ::onFolderClick,
                                onShowAllCLick = ::onShowAllClick,
                                onFollowPodcast = ::onSubscribeToPodcast,
                                onScroll = { UiUtil.hideKeyboard(searchView) },
                                bottomInset = bottomInset.pxToDp(LocalContext.current).dp,
                            )

                        else -> Spacer(modifier = Modifier.size(0.dp))
                    }
                }
                binding.searchInlineResults.isVisible = (state is SearchUiState.OldResults || state is SearchUiState.ImprovedResults) && !state.searchTerm.isNullOrBlank()
            }
        }
    }

    private fun onEpisodeClick(episodeItem: EpisodeItem) {
        onEpisodeClick(SearchHistoryEntry.fromEpisode(episodeItem.toEpisode(), episodeItem.podcastTitle))
    }

    private fun onEpisodeClick(episode: SearchHistoryEntry.Episode) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = episode.uuid,
            type = SearchResultType.EPISODE,
        )
        searchHistoryViewModel.add(episode)
        listener?.onSearchEpisodeClick(
            episodeUuid = episode.uuid,
            podcastUuid = episode.podcastUuid,
            source = EpisodeViewSource.SEARCH,
        )
        binding?.searchView?.let { UiUtil.hideKeyboard(it) }
    }

    private fun onPodcastClick(podcast: SearchHistoryEntry.Podcast, isSubscribed: Boolean) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = podcast.uuid,
            type = if (onlySearchRemote || !isSubscribed) {
                SearchResultType.PODCAST_REMOTE_RESULT
            } else {
                SearchResultType.PODCAST_LOCAL_RESULT
            },
        )
        searchHistoryViewModel.add(podcast)
        listener?.onSearchPodcastClick(podcast.uuid, SourceView.SEARCH)
        binding?.searchView?.let { UiUtil.hideKeyboard(it) }
    }

    private fun onSubscribeToPodcast(podcast: Podcast) {
        viewModel.onSubscribeToPodcast(podcast)
    }

    private fun onFolderClick(folder: Folder, podcasts: List<Podcast>) {
        onFolderClick(SearchHistoryEntry.fromFolder(folder, podcasts.map { it.uuid }))
    }

    private fun onFolderClick(folder: SearchHistoryEntry.Folder) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = folder.uuid,
            type = SearchResultType.FOLDER,
        )
        searchHistoryViewModel.add(folder)
        listener?.onSearchFolderClick(folder.uuid)
        binding?.searchView?.let { UiUtil.hideKeyboard(it) }
    }

    private fun onShowAllClick(resultsType: ResultsType) {
        viewModel.trackSearchListShown(source, resultsType)
        val fragment = SearchResultsFragment.newInstance(resultsType, onlySearchRemote, source)
        childFragmentManager.beginTransaction()
            .replace(R.id.searchResults, fragment)
            .addToBackStack(SEARCH_RESULTS_TAG)
            .commit()
    }

    override fun onBackPressed(): Boolean {
        viewModel.trackSearchShownOrDismissed(AnalyticsEvent.SEARCH_DISMISSED, source)
        return super.onBackPressed()
    }
}
