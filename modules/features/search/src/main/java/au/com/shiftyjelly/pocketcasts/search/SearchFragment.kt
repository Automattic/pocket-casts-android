package au.com.shiftyjelly.pocketcasts.search

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.search.SearchViewModel.SearchResultType
import au.com.shiftyjelly.pocketcasts.search.adapter.PodcastSearchAdapter
import au.com.shiftyjelly.pocketcasts.search.databinding.FragmentSearchBinding
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryClearAllConfirmationDialog
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryPage
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.extensions.showKeyboard
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ARG_FLOATING = "arg_floating"
private const val ARG_ONLY_SEARCH_REMOTE = "arg_only_search_remote"
private const val ARG_SOURCE = "arg_source"
private const val SEARCH_HISTORY_CLEAR_ALL_CONFIRMATION_DIALOG_TAG = "search_history_clear_all_confirmation_dialog"

@AndroidEntryPoint
class SearchFragment : BaseFragment() {
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    interface Listener {
        fun onSearchPodcastClick(podcastUuid: String)
        fun onSearchFolderClick(folderUuid: String)
    }

    companion object {
        fun newInstance(
            floating: Boolean = false,
            onlySearchRemote: Boolean = false,
            source: AnalyticsSource
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
    val source: AnalyticsSource
        get() = AnalyticsSource.fromString(arguments?.getString(ARG_SOURCE))

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                UiUtil.hideKeyboard(recyclerView)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as Listener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner { viewLifecycleOwner.lifecycle }
        viewModel.setOnlySearchRemote(onlySearchRemote)
        searchHistoryViewModel.setOnlySearchRemote(onlySearchRemote)
        binding.viewModel = viewModel
        binding.floating = floating

        this.binding = binding

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        binding?.let {
            UiUtil.hideKeyboard(it.searchView)
        }
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun navigateFromSearchHistoryEntry(entry: SearchHistoryEntry) {
        when (entry) {
            is SearchHistoryEntry.Episode -> Unit // TODO
            is SearchHistoryEntry.Folder -> listener?.onSearchFolderClick(entry.uuid)
            is SearchHistoryEntry.Podcast -> listener?.onSearchPodcastClick(entry.uuid)
            is SearchHistoryEntry.SearchTerm -> binding?.searchView?.setQuery(entry.term, true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        view.setOnClickListener {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }
        binding.resultPanel.alpha = 0.0f
        binding.resultPanel.animate().setDuration(500L).alpha(1.0f).start()

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
        }

        val searchManager = view.context.getSystemService(Activity.SEARCH_SERVICE) as SearchManager
        activity?.let { searchView.setSearchableInfo(searchManager.getSearchableInfo(it.componentName)) }
        searchView.queryHint = getString(LR.string.search_podcasts)
        searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        searchView.setIconifiedByDefault(false)
        // seems like a more reliable focus using a post
        searchView.post {
            searchView.showKeyboard()
        }
        searchView.setOnCloseListener {
            UiUtil.hideKeyboard(searchView)
            true
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.updateSearchQuery(query)
                binding.searchHistoryPanel.hide()
                UiUtil.hideKeyboard(searchView)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                // don't auto search for feed URLs
                val characterCount = query.length
                val lowerCaseSearch = query.lowercase()
                if ((characterCount == 1 && lowerCaseSearch.startsWith("h")) || (characterCount == 2 && lowerCaseSearch.startsWith("ht")) || (characterCount == 3 && lowerCaseSearch.startsWith("htt")) || lowerCaseSearch.startsWith("http")) {
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

        val searchAdapter = PodcastSearchAdapter(
            theme = theme,
            onPodcastClick = { podcast ->
                viewModel.trackSearchResultTapped(
                    source = source,
                    uuid = podcast.uuid,
                    type = if (onlySearchRemote || !podcast.isSubscribed) {
                        SearchResultType.PODCAST_REMOTE_RESULT
                    } else {
                        SearchResultType.PODCAST_LOCAL_RESULT
                    }
                )
                searchHistoryViewModel.add(SearchHistoryEntry.fromPodcast(podcast))
                listener?.onSearchPodcastClick(podcast.uuid)
                UiUtil.hideKeyboard(searchView)
            },
            onFolderClick = { folder, podcasts ->
                viewModel.trackSearchResultTapped(
                    source = source,
                    uuid = folder.uuid,
                    type = SearchResultType.FOLDER,
                )
                searchHistoryViewModel.add(SearchHistoryEntry.fromFolder(folder, podcasts.map { it.uuid }))
                listener?.onSearchFolderClick(folder.uuid)
                UiUtil.hideKeyboard(searchView)
            }
        )

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = searchAdapter
        recyclerView.itemAnimator = null
        recyclerView.addOnScrollListener(onScrollListener)

        binding.searchHistoryPanel.setContent {
            AppTheme(theme.activeTheme) {
                SearchHistoryPage(
                    viewModel = searchHistoryViewModel,
                    onClick = ::navigateFromSearchHistoryEntry,
                    onShowClearAllConfirmation = {
                        SearchHistoryClearAllConfirmationDialog(
                            context = this@SearchFragment.requireContext(),
                            onConfirm = { searchHistoryViewModel.clearAll() }
                        ).show(parentFragmentManager, SEARCH_HISTORY_CLEAR_ALL_CONFIRMATION_DIALOG_TAG)
                    }
                )
                if (viewModel.isFragmentChangingConfigurations && viewModel.showSearchHistory) {
                    binding.searchHistoryPanel.show()
                }
            }
        }

        val noResultsView = binding.noResults
        val searchFailedView = binding.searchFailed
        viewModel.searchResults.observe(viewLifecycleOwner) {
            noResultsView.hide()
            searchFailedView.hide()
            recyclerView.hide()
            when (it) {
                is SearchState.NoResults -> {
                    searchAdapter.submitList(emptyList())
                    noResultsView.show()
                }
                is SearchState.Results -> {
                    searchAdapter.submitList(it.list)
                    if (it.error == null || !onlySearchRemote || it.loading) {
                        recyclerView.show()
                    } else {
                        searchFailedView.show()
                    }
                }
            }
        }
    }
}
