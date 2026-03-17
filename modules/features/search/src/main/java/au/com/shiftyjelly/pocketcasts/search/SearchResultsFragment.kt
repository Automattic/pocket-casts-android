package au.com.shiftyjelly.pocketcasts.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.search.searchhistory.SearchHistoryViewModel
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.automattic.eventhorizon.SearchResultLegacyType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class SearchResultsFragment : BaseFragment() {
    @Inject lateinit var settings: Settings

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARGS)

    private val viewModel by viewModels<SearchViewModel>({ requireParentFragment() })

    private val searchHistoryViewModel by viewModels<SearchHistoryViewModel>()

    private var listener: SearchFragment.Listener? = null

    @SuppressLint("MissingSuperCall") // False positive
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
    ) = contentWithoutConsumedInsets {
        val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
        val bottomInsetDp = bottomInset.pxToDp(LocalContext.current).dp
        AppThemeWithBackground(theme.activeTheme) {
            when (args.type) {
                ResultsType.PODCASTS -> {
                    SearchPodcastResultsPage(
                        viewModel = viewModel,
                        onFolderClick = ::onFolderClick,
                        onPodcastClick = ::onPodcastClick,
                        onBackPress = ::onBackPress,
                        bottomInset = bottomInsetDp,
                    )
                }

                ResultsType.EPISODES -> {
                    SearchEpisodeResultsPage(
                        viewModel = viewModel,
                        onBackPress = ::onBackPress,
                        onEpisodeClick = ::onEpisodeClick,
                        bottomInset = bottomInsetDp,
                    )
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
            source = args.source,
            uuid = episodeItem.uuid,
            type = SearchViewModel.SearchResultType.EPISODE,
        )
        val episode = episodeItem.toEpisode()
        searchHistoryViewModel.add(SearchHistoryEntry.fromEpisode(episode, episodeItem.podcastTitle))
        listener?.onSearchEpisodeClick(
            episodeUuid = episode.uuid,
            podcastUuid = episode.podcastUuid,
            source = EpisodeViewSource.SEARCH,
        )
    }

    private fun onFolderClick(folder: Folder, podcasts: List<Podcast>) {
        viewModel.trackSearchResultTapped(
            source = args.source,
            uuid = folder.uuid,
            type = SearchViewModel.SearchResultType.FOLDER,
        )
        searchHistoryViewModel.add(SearchHistoryEntry.fromFolder(folder, podcasts.map { it.uuid }))
        listener?.onSearchFolderClick(folder.uuid)
    }

    private fun onPodcastClick(podcast: Podcast) {
        viewModel.trackSearchResultTapped(
            source = args.source,
            uuid = podcast.uuid,
            type = if (args.onlySearchRemote || !podcast.isSubscribed) {
                SearchViewModel.SearchResultType.PODCAST_REMOTE_RESULT
            } else {
                SearchViewModel.SearchResultType.PODCAST_LOCAL_RESULT
            },
        )
        searchHistoryViewModel.add(SearchHistoryEntry.fromPodcast(podcast))
        listener?.onSearchPodcastClick(podcast.uuid, SourceView.SEARCH_RESULTS)
    }

    private fun onBackPress() {
        parentFragmentManager.popBackStack()
    }

    @Parcelize
    private class Args(
        val type: ResultsType,
        val onlySearchRemote: Boolean,
        val source: SourceView,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "new_instance_args"

        enum class ResultsType(
            val analyticsValue: SearchResultLegacyType,
        ) {
            PODCASTS(
                analyticsValue = SearchResultLegacyType.Podcasts,
            ),
            EPISODES(
                analyticsValue = SearchResultLegacyType.Episodes,
            ),
        }

        fun newInstance(
            type: ResultsType,
            onlySearchRemote: Boolean,
            source: SourceView,
        ) = SearchResultsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    NEW_INSTANCE_ARGS,
                    Args(
                        type = type,
                        onlySearchRemote = onlySearchRemote,
                        source = source,
                    ),
                )
            }
        }
    }
}
