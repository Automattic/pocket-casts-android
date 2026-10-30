package au.com.shiftyjelly.pocketcasts.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.search.SearchViewModel.SearchResultType
import au.com.shiftyjelly.pocketcasts.search.component.SearchToolbar
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryClearAllConfirmationDialog
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryPage
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SearchClearedEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_FLOATING = "arg_floating"
private const val ARG_ONLY_SEARCH_REMOTE = "arg_only_search_remote"
private const val ARG_SOURCE = "arg_source"
private const val STATE_QUERY = "state_query"
private const val SEARCH_HISTORY_CLEAR_ALL_CONFIRMATION_DIALOG_TAG = "search_history_clear_all_confirmation_dialog"

@AndroidEntryPoint
class SearchFragment : BaseFragment() {
    @Inject
    lateinit var eventHorizon: EventHorizon

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
                putString(ARG_SOURCE, source.key)
            }
            fragment.arguments = arguments
            return fragment
        }
    }

    private val viewModel: SearchViewModel by viewModels()
    private val searchHistoryViewModel: SearchHistoryViewModel by viewModels()
    private var listener: Listener? = null
    private var query by mutableStateOf("")
    private var isHistoryVisible by mutableStateOf(true)
    private var isKeyboardRequested = false

    val floating: Boolean
        get() = arguments?.getBoolean(ARG_FLOATING) ?: false
    val onlySearchRemote: Boolean
        get() = arguments?.getBoolean(ARG_ONLY_SEARCH_REMOTE) ?: false
    val source: SourceView
        get() = SourceView.fromString(arguments?.getString(ARG_SOURCE))

    @SuppressLint("MissingSuperCall") // False positive
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
        query = savedInstanceState?.getString(STATE_QUERY).orEmpty()

        viewModel.trackSearchShown(source)
        playButtonListener.source = source
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.setOnlySearchRemote(onlySearchRemote)
        searchHistoryViewModel.setOnlySearchRemote(onlySearchRemote)
        searchHistoryViewModel.setSource(source)
        return ComposeView(requireContext()).apply {
            setContentWithViewCompositionStrategy {
                AppTheme(theme.activeTheme) {
                    SearchContent()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_QUERY, query)
    }

    override fun onPause() {
        super.onPause()
        view?.let {
            UiUtil.hideKeyboard(it)
            it.clearFocus()
        }
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun hideKeyboard() {
        view?.let(UiUtil::hideKeyboard)
    }

    private fun navigateFromSearchHistoryEntry(entry: SearchHistoryEntry) {
        searchHistoryViewModel.trackHistoryItemTapped(entry)
        when (entry) {
            is SearchHistoryEntry.Episode -> listener?.onSearchEpisodeClick(
                episodeUuid = entry.uuid,
                podcastUuid = entry.podcastUuid,
                source = EpisodeViewSource.SEARCH_HISTORY,
            )

            is SearchHistoryEntry.Folder -> listener?.onSearchFolderClick(entry.uuid)

            is SearchHistoryEntry.Podcast -> listener?.onSearchPodcastClick(entry.uuid, SourceView.SEARCH)

            is SearchHistoryEntry.SearchTerm -> {
                viewModel.runSearchOnTerm(entry.term)
                onQueryTextChange(entry.term)
                onQueryTextSubmit(entry.term)
                isHistoryVisible = false
                hideKeyboard()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setSource(source)
        isKeyboardRequested = viewModel.state.value.searchTerm.isNullOrBlank()
        isHistoryVisible = true
        if (query.isNotEmpty()) {
            onQueryTextChange(query)
        }

        lifecycleScope.launch {
            // detect when we select a suggestion
            viewModel.state
                .scan<SearchUiState, Pair<SearchUiState?, SearchUiState?>>(null to null) { acc, value -> acc.second to value }
                .drop(1)
                .filter { (previous, next) -> previous is SearchUiState.Suggestions && next is SearchUiState.Results }
                .mapNotNull { (_, next) -> next?.searchTerm }
                .collect {
                    if (it != query) {
                        onQueryTextChange(it)
                    }
                }
        }
    }

    private fun onSearchAction() {
        if (query.isNotBlank()) {
            viewModel.runSearchOnTerm(query)
            isHistoryVisible = false
            hideKeyboard()
        }
    }

    private fun onClearClick() {
        eventHorizon.track(
            SearchClearedEvent(
                source = source.analyticsValue,
            ),
        )
        onQueryTextChange("")
    }

    private fun onQueryTextSubmit(query: String) {
        viewModel.updateSearchQuery(query)
        isHistoryVisible = false
        hideKeyboard()
    }

    private fun onQueryTextChange(query: String) {
        this.query = query
        // don't auto search for feed URLs
        val characterCount = query.length
        val lowerCaseSearch = query.lowercase()
        if ((characterCount == 1 && lowerCaseSearch.startsWith("h")) || (characterCount == 2 && lowerCaseSearch.startsWith("ht")) || (characterCount == 3 && lowerCaseSearch.startsWith("htt")) || lowerCaseSearch.startsWith("http")) {
            return
        }
        viewModel.updateSearchQuery(query)
        isHistoryVisible = characterCount == 0
    }

    @Composable
    private fun SearchContent() {
        val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
        val state by viewModel.state.collectAsState()
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            if (isKeyboardRequested) {
                focusRequester.requestFocus()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.theme.colors.primaryUi01),
        ) {
            SearchToolbar(
                query = query,
                isFloating = floating,
                focusRequester = focusRequester,
                onQueryChange = ::onQueryTextChange,
                onSearch = ::onSearchAction,
                onClear = ::onClearClick,
                onBack = { activity?.onBackPressedDispatcher?.onBackPressed() },
            )

            Box(modifier = Modifier.gone(!isHistoryVisible)) {
                if (state is SearchUiState.Suggestions && state.searchTerm.isNullOrBlank()) {
                    searchHistoryViewModel.start()
                }
                SearchHistoryPage(
                    viewModel = searchHistoryViewModel,
                    onClick = ::navigateFromSearchHistoryEntry,
                    onShowClearAllConfirmation = {
                        SearchHistoryClearAllConfirmationDialog(
                            context = this@SearchFragment.requireContext(),
                            onConfirm = { searchHistoryViewModel.clearAll() },
                        ).show(parentFragmentManager, SEARCH_HISTORY_CLEAR_ALL_CONFIRMATION_DIALOG_TAG)
                    },
                    onScroll = ::hideKeyboard,
                )
                SideEffect {
                    if (viewModel.isFragmentChangingConfigurations && viewModel.showSearchHistory) {
                        isHistoryVisible = true
                    }
                }
            }

            Box(modifier = Modifier.gone(state !is SearchUiState.Suggestions || state.searchTerm.isNullOrBlank())) {
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
                            onScroll = ::hideKeyboard,
                            bottomInset = bottomInset.pxToDp(LocalContext.current).dp,
                            isLoading = suggestions.operation is SearchUiState.SearchOperation.Loading,
                            isError = suggestions.operation is SearchUiState.SearchOperation.Error,
                            onReportSuggestionsRender = viewModel::trackSuggestionsShown,
                            onReportViewAllClick = viewModel::onViewAllSuggestionsClick,
                        )
                    }
                }
            }

            Box(modifier = Modifier.gone(state !is SearchUiState.Results || state.searchTerm.isNullOrBlank())) {
                AppThemeWithBackground(theme.activeTheme) {
                    when (val state = state) {
                        is SearchUiState.Results ->
                            ImprovedSearchResultsPage(
                                state = state,
                                loading = state.isLoading,
                                onEpisodeClick = { onEpisodeClick(episode = SearchHistoryEntry.fromImprovedEpisodeResult(it)) },
                                onPodcastClick = { onPodcastClick(SearchHistoryEntry.fromImprovedPodcastResult(it), it.isFollowed) },
                                onFolderClick = ::onFolderClick,
                                onNetworkClick = ::onNetworkClick,
                                onFollowPodcast = { viewModel.onSubscribeToPodcast(it.uuid) },
                                playButtonListener = playButtonListener,
                                onScroll = ::hideKeyboard,
                                bottomInset = bottomInset.pxToDp(LocalContext.current).dp,
                                onFilterSelect = viewModel::selectFilter,
                                onResultsShow = viewModel::reportResultsShown,
                                onEmptyResultsShow = viewModel::reportEmptyResultsShown,
                                onErrorShow = viewModel::reportErrorResultsShown,
                            )

                        else -> Spacer(modifier = Modifier.size(0.dp))
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { activity?.onBackPressedDispatcher?.onBackPressed() }
                    },
            )
        }
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
        hideKeyboard()
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
        hideKeyboard()
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
        hideKeyboard()
    }

    private fun onNetworkClick(network: ImprovedSearchResultItem.NetworkItem) {
        viewModel.trackSearchResultTapped(
            source = source,
            uuid = network.uuid,
            type = SearchResultType.NETWORK,
        )
        (requireActivity() as FragmentHostListener).openNetworkPage(
            listId = network.uuid,
            title = network.title,
            sourceView = SourceView.SEARCH_RESULTS,
        )
        hideKeyboard()
    }

    override fun onBackPressed(): Boolean {
        viewModel.trackSearchDismissed(source)
        return super.onBackPressed()
    }
}

private fun Modifier.gone(isGone: Boolean) = if (isGone) layout { _, _ -> layout(0, 0) {} } else this
