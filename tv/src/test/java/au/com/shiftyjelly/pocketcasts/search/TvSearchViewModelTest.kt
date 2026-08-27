package au.com.shiftyjelly.pocketcasts.search

import android.content.Context
import android.content.res.Resources
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.search.ImprovedSearchManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.DiscoverCategoriesPillTappedEvent
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SearchEmptyResultsEvent
import com.automattic.eventhorizon.SearchFailedEvent
import com.automattic.eventhorizon.SearchFilterTappedEvent
import com.automattic.eventhorizon.SearchHistoryItemTappedEvent
import com.automattic.eventhorizon.SearchHistoryType
import com.automattic.eventhorizon.SearchPerformedEvent
import com.automattic.eventhorizon.SearchPredictiveTermTappedEvent
import com.automattic.eventhorizon.SearchResultFilterType
import com.automattic.eventhorizon.SearchResultTappedEvent
import com.automattic.eventhorizon.SearchResultType
import com.automattic.eventhorizon.SearchShownEvent
import com.automattic.eventhorizon.SourceViewType
import io.reactivex.Single
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSearchViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
    }
    private val resources = mock<Resources>()
    private val context = mock<Context> {
        whenever(it.resources).thenReturn(resources)
    }
    private val discoverCountryCode = mock<UserSetting<String>> {
        whenever(it.value).thenReturn("us")
    }
    private val settings = mock<Settings> {
        whenever(it.discoverCountryCode).thenReturn(discoverCountryCode)
    }
    private val improvedSearchManager = mock<ImprovedSearchManager>()
    private val podcastManager = mock<PodcastManager> {
        whenever(it.findSubscribedFlow(any())).thenReturn(flowOf(emptyList()))
    }
    private val episodeManager = mock<EpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val searchHistoryManager = mock<SearchHistoryManager>()
    private val folderManager = mock<FolderManager>()
    private val eventHorizon = mock<EventHorizon>()

    init {
        whenever { improvedSearchManager.autoCompleteSearch(any()) }.thenReturn(emptyList())
        whenever { searchHistoryManager.findAll(any()) }.thenReturn(emptyList())
    }

    @Test
    fun `exposes the browse categories from the search feed row`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(categoriesRow(source = "https://categories.json")),
        )
        whenever(listRepository.getCategoriesList(eq("https://categories.json")))
            .thenReturn(listOf(category(1, "Comedy"), category(2, "True Crime")))

        val viewModel = createViewModel()

        assertEquals(listOf("Comedy", "True Crime"), viewModel.categories.value.map { it.name })
    }

    @Test
    fun `categories are empty when the feed has no categories row`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(row(id = "trending", title = "Trending", source = "https://lists/trending.json")),
        )

        val viewModel = createViewModel()

        assertTrue(viewModel.categories.value.isEmpty())
    }

    @Test
    fun `categories are empty when the categories request fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(categoriesRow(source = "https://categories.json")),
        )
        whenever(listRepository.getCategoriesList(any())).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        assertTrue(viewModel.categories.value.isEmpty())
    }

    @Test
    fun `exposes the loaded discover rows`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(row(id = "trending", title = "Trending", source = "https://lists/trending.json")),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        val rows = viewModel.discoverRows.value
        assertEquals(listOf("trending"), rows.map { it.id })
        assertTrue(rows.single() is TvDiscoverRow.Podcasts)
    }

    @Test
    fun `banner rows are excluded from the search discover feed`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                bannerRow(id = "create_account"),
                row(id = "trending", title = "Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        assertEquals(listOf("trending"), viewModel.discoverRows.value.map { it.id })
    }

    @Test
    fun `authenticated rows are dropped when logged out`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                row(id = "public", title = "Public", source = "https://lists/public.json"),
                row(id = "members", title = "Members", source = "https://lists/members.json", authenticated = true),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/public.json"), any()))
            .thenReturn(podcastFeed("podcast-public"))
        whenever(listRepository.getListFeed(eq("https://lists/members.json"), any()))
            .thenReturn(podcastFeed("podcast-members"))

        val viewModel = createViewModel()

        assertEquals(listOf("public"), viewModel.discoverRows.value.map { it.id })
        verify(listRepository, never()).getListFeed(eq("https://lists/members.json"), any())
    }

    @Test
    fun `authenticated rows are kept when logged in`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                row(id = "public", title = "Public", source = "https://lists/public.json"),
                row(id = "members", title = "Members", source = "https://lists/members.json", authenticated = true),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/public.json"), any()))
            .thenReturn(podcastFeed("podcast-public"))
        whenever(listRepository.getListFeed(eq("https://lists/members.json"), any()))
            .thenReturn(podcastFeed("podcast-members"))

        val viewModel = createViewModel()

        assertEquals(listOf("public", "members"), viewModel.discoverRows.value.map { it.id })
    }

    @Test
    fun `single podcast display style maps to a single podcast row`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                row(
                    id = "spotlight",
                    title = "Spotlight",
                    source = "https://lists/spotlight.json",
                    displayStyle = DisplayStyle.SinglePodcast(),
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/spotlight.json"), any()))
            .thenReturn(podcastFeed("podcast-spotlight"))

        val viewModel = createViewModel()

        assertTrue(viewModel.discoverRows.value.single() is TvDiscoverRow.SinglePodcast)
    }

    @Test
    fun `discover rows are empty when loading fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        assertTrue(viewModel.discoverRows.value.isEmpty())
    }

    @Test
    fun `categoryPodcasts maps the loaded category feed`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(listRepository.getListFeed(eq("https://category/us.json"), any()))
            .thenReturn(podcastFeed("podcast-1", "podcast-2"))

        val podcasts = createViewModel().categoryPodcasts(categoryId = 7, source = "https://category/us.json").podcasts

        assertEquals(listOf("podcast-1", "podcast-2"), podcasts.map { it.uuid })
    }

    @Test
    fun `categories still load when building the discover rows fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            Discover(
                layout = listOf(categoriesRow(source = "https://categories.json")),
                regions = emptyMap(),
                regionCodeToken = "[regionCode]",
                regionNameToken = "[regionName]",
                defaultRegionCode = "us",
            ),
        )
        whenever(listRepository.getCategoriesList(eq("https://categories.json")))
            .thenReturn(listOf(category(1, "Comedy")))

        val viewModel = createViewModel()

        assertEquals(listOf("Comedy"), viewModel.categories.value.map { it.name })
        assertTrue(viewModel.discoverRows.value.isEmpty())
    }

    @Test
    fun `a blank query stays idle`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onQueryChange("   ")
        advanceUntilIdle()

        assertEquals(TvSearchState.Idle, viewModel.searchState.value)
    }

    @Test
    fun `combined results are split into podcasts and episodes`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(
            listOf(podcastItem("podcast-1"), episodeItem("episode-1")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("podcast-1"), state.podcasts.map { it.uuid })
        assertEquals(listOf("episode-1"), state.episodes.map { it.uuid })
    }

    @Test
    fun `episodes are de-duplicated by uuid`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(
            listOf(episodeItem("episode-1"), episodeItem("episode-1"), episodeItem("episode-2")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("episode-1", "episode-2"), state.episodes.map { it.uuid })
    }

    @Test
    fun `keystrokes within the debounce window only search the last term`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("su")
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        verifyBlocking(improvedSearchManager) { combinedSearch(eq("sugar")) }
        verifyBlocking(improvedSearchManager, never()) { combinedSearch(eq("su")) }
    }

    @Test
    fun `empty combined results report no results`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(TvSearchState.NoResults, viewModel.searchState.value)
    }

    @Test
    fun `server results lead and subscribed podcasts fill the remaining gaps`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever(podcastManager.findSubscribedFlow(any())).thenReturn(flowOf(listOf(subscribedPodcast("podcast-1"))))
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(
            listOf(podcastItem("podcast-2")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("podcast-2", "podcast-1"), state.podcasts.map { it.uuid })
    }

    @Test
    fun `predictive term results are exposed as suggestions`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.autoCompleteSearch(any()) }.thenReturn(
            listOf(SearchAutoCompleteItem.Term("sugar rush"), SearchAutoCompleteItem.Term("sugar high")),
        )
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(listOf("sugar rush", "sugar high"), viewModel.suggestions.value)
    }

    @Test
    fun `saving a search term persists it and exposes recent searches`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { searchHistoryManager.findAll(any()) }.thenReturn(
            listOf(SearchHistoryEntry.SearchTerm(term = "sugar")),
        )

        val viewModel = createViewModel()
        viewModel.saveSearchTerm("sugar")
        advanceUntilIdle()

        verifyBlocking(searchHistoryManager) { add(any()) }
        assertEquals(listOf("sugar"), viewModel.history.value)
    }

    @Test
    fun `a failure fetching subscribed podcasts surfaces the error state`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever(podcastManager.findSubscribedFlow(any())).thenThrow(RuntimeException("database unavailable"))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(TvSearchState.Error, viewModel.searchState.value)
    }

    @Test
    fun `searching does not save partial terms to history`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sug")
        advanceUntilIdle()

        verifyBlocking(searchHistoryManager, never()) { add(any()) }
    }

    @Test
    fun `clearing the query clears the suggestions`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.autoCompleteSearch(any()) }.thenReturn(
            listOf(SearchAutoCompleteItem.Term("sugar")),
        )
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()
        assertTrue(viewModel.suggestions.value.isNotEmpty())

        viewModel.onQueryChange("")

        assertTrue(viewModel.suggestions.value.isEmpty())
    }

    @Test
    fun `a search failure surfaces the error state`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(TvSearchState.Error, viewModel.searchState.value)
    }

    @Test
    fun `onFilterSelected updates the filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Episodes)

        assertEquals(TvSearchFilter.Episodes, viewModel.filter.value)
    }

    @Test
    fun `clearing the query preserves the selected filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Podcasts)
        viewModel.onQueryChange("")
        advanceUntilIdle()

        assertEquals(TvSearchFilter.Podcasts, viewModel.filter.value)
    }

    @Test
    fun `showing the search screen tracks a search shown event`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.trackSearchShown()

        verify(eventHorizon).track(SearchShownEvent(source = SourceViewType.Search))
    }

    @Test
    fun `running a search tracks a search performed event`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(listOf(podcastItem("podcast-1")))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        verify(eventHorizon).track(SearchPerformedEvent(source = SourceViewType.Search))
    }

    @Test
    fun `empty results track a search empty results event with the term`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        verify(eventHorizon).track(SearchEmptyResultsEvent(source = SourceViewType.Search, term = "sugar"))
    }

    @Test
    fun `a search failure tracks a search failed event with the term`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        verify(eventHorizon).track(SearchFailedEvent(source = SourceViewType.Search, term = "sugar"))
    }

    @Test
    fun `selecting a filter tracks a search filter tapped event`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Episodes)

        assertEquals(TvSearchFilter.Episodes, viewModel.filter.value)
        verify(eventHorizon).track(SearchFilterTappedEvent(source = SourceViewType.Search, filter = SearchResultFilterType.Episodes))
    }

    @Test
    fun `selecting the folders filter tracks a search filter tapped event with the folders filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Folders)

        assertEquals(TvSearchFilter.Folders, viewModel.filter.value)
        verify(eventHorizon).track(SearchFilterTappedEvent(source = SourceViewType.Search, filter = SearchResultFilterType.Folders))
    }

    @Test
    fun `re-selecting the current filter does not track another filter tapped event`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Podcasts)
        viewModel.onFilterSelected(TvSearchFilter.Podcasts)

        assertEquals(TvSearchFilter.Podcasts, viewModel.filter.value)
        verify(eventHorizon, times(1)).track(SearchFilterTappedEvent(source = SourceViewType.Search, filter = SearchResultFilterType.Podcasts))
    }

    @Test
    fun `selecting a suggestion tracks a predictive term tapped event and runs the search`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.selectSuggestion("sugar rush")
        advanceUntilIdle()

        verify(eventHorizon).track(SearchPredictiveTermTappedEvent(source = SourceViewType.Search, term = "sugar rush"))
        assertEquals("sugar rush", viewModel.query.value)
    }

    @Test
    fun `selecting a history item tracks a history item tapped event and runs the search`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.selectHistoryItem("sugar")
        advanceUntilIdle()

        verify(eventHorizon).track(SearchHistoryItemTappedEvent(source = SourceViewType.Search, type = SearchHistoryType.SearchTerm))
        assertEquals("sugar", viewModel.query.value)
    }

    @Test
    fun `tapping a subscribed podcast result tracks a local result`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.trackPodcastResultTapped(podcastItem("podcast-1").copy(isFollowed = true))

        verify(eventHorizon).track(
            SearchResultTappedEvent(source = SourceViewType.Search, uuid = "podcast-1", resultType = SearchResultType.PodcastLocalResult),
        )
    }

    @Test
    fun `tapping an unsubscribed podcast result tracks a remote result`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.trackPodcastResultTapped(podcastItem("podcast-1"))

        verify(eventHorizon).track(
            SearchResultTappedEvent(source = SourceViewType.Search, uuid = "podcast-1", resultType = SearchResultType.PodcastRemoteResult),
        )
    }

    @Test
    fun `tapping an episode result tracks an episode result tapped event`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.trackEpisodeResultTapped("episode-1")

        verify(eventHorizon).track(
            SearchResultTappedEvent(source = SourceViewType.Search, uuid = "episode-1", resultType = SearchResultType.Episode),
        )
    }

    @Test
    fun `tapping a browse category stamps the search source on the pill event`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()
        val category = category(1, "Comedy")

        viewModel.trackCategoryPillTapped(category, index = 3)

        verify(eventHorizon).track(
            DiscoverCategoriesPillTappedEvent(
                name = "Comedy",
                region = "us",
                index = 3,
                visits = category.totalVisits.toLong(),
                sponsored = category.isSponsored ?: false,
                source = "search",
            ),
        )
    }

    @Test
    fun `a discover row impression in the idle body is stamped with the search source`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val row = TvDiscoverRow.Podcasts(id = "list-trending", title = "Trending", podcasts = listOf(discoverPodcast("podcast-1")))

        createViewModel().trackDiscoverListShown(row)

        verify(eventHorizon).track(DiscoverListImpressionEvent(listId = "list-trending", source = "search"))
    }

    @Test
    fun `opening a discover podcast is stamped with the search source`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val podcast = discoverPodcast("podcast-1")
        val row = TvDiscoverRow.Podcasts(id = "list-trending", title = "Trending", podcasts = listOf(podcast))

        createViewModel().trackDiscoverPodcastTapped(row, podcast)

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "list-trending", podcastUuid = "podcast-1", source = "search"))
    }

    @Test
    fun `opening a discover episode's podcast is stamped with the search source`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val episode = TvDiscoverEpisode("episode-1", "Episode", "podcast-1", "Podcast")
        val row = TvDiscoverRow.Episodes(id = "list-videos", title = "Made for TV", episodes = listOf(episode))

        createViewModel().trackDiscoverEpisodePodcastTapped(row, episode)

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "list-videos", podcastUuid = "podcast-1", source = "search"))
    }

    @Test
    fun `search does not suppress the home local row ids`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val row = TvDiscoverRow.Podcasts(id = "keep_listening", title = "Keep Listening", podcasts = listOf(discoverPodcast("podcast-1")))

        createViewModel().trackDiscoverListShown(row)

        verify(eventHorizon).track(DiscoverListImpressionEvent(listId = "keep_listening", source = "search"))
    }

    @Test
    fun `playDiscoverEpisode plays an episode that is already in the database`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val episode = podcastEpisode("episode-1")
        whenever(episodeManager.findByUuid("episode-1")).thenReturn(episode)

        createViewModel().playDiscoverEpisode(discoverEpisode("episode-1"))

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.DISCOVER) }
        verify(podcastManager, never()).findOrDownloadPodcastRxSingle(any(), any())
    }

    @Test
    fun `playDiscoverEpisode fetches the podcast before playing an unknown episode`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val episode = podcastEpisode("episode-1")
        whenever(episodeManager.findByUuid("episode-1")).thenReturn(null, episode)
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast-episode-1")).thenReturn(Single.just(subscribedPodcast("podcast-episode-1")))

        createViewModel().playDiscoverEpisode(discoverEpisode("episode-1"))

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.DISCOVER) }
    }

    @Test
    fun `playLatestEpisode plays the newest episode of a featured podcast and stamps the search source`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val podcast = subscribedPodcast("podcast-1")
        val newest = podcastEpisode("episode-new")
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast-1")).thenReturn(Single.just(podcast))
        whenever(episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast))
            .thenReturn(listOf(newest, podcastEpisode("episode-old")))
        val row = TvDiscoverRow.FeaturedPodcasts(id = "list-featured", title = "Featured", podcasts = listOf(discoverPodcast("podcast-1")))

        createViewModel().playLatestEpisode(row, discoverPodcast("podcast-1"))

        verifyBlocking(playbackManager) { playNowSuspend(episode = newest, sourceView = SourceView.DISCOVER) }
        verify(eventHorizon).track(
            DiscoverListEpisodeTappedEvent(listId = "list-featured", podcastUuid = "podcast-1", episodeUuid = "episode-new", source = "search"),
        )
        verify(eventHorizon).track(DiscoverListEpisodePlayEvent(listId = "list-featured", podcastUuid = "podcast-1"))
    }

    @Test
    fun `playLatestEpisode reports a failure when the podcast has no episodes`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val podcast = subscribedPodcast("podcast-1")
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast-1")).thenReturn(Single.just(podcast))
        whenever(episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)).thenReturn(emptyList())
        val row = TvDiscoverRow.FeaturedPodcasts(id = "list-featured", title = "Featured", podcasts = listOf(discoverPodcast("podcast-1")))
        val viewModel = createViewModel()

        viewModel.playFailures.test {
            viewModel.playLatestEpisode(row, discoverPodcast("podcast-1"))

            awaitItem()
            verifyNoInteractions(playbackManager)
        }
    }

    @Test
    fun `playLatestEpisode records the tap even when playback fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val podcast = subscribedPodcast("podcast-1")
        val newest = podcastEpisode("episode-new")
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast-1")).thenReturn(Single.just(podcast))
        whenever(episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)).thenReturn(listOf(newest))
        whenever { playbackManager.playNowSuspend(episode = newest, sourceView = SourceView.DISCOVER) }
            .thenThrow(RuntimeException("boom"))
        val row = TvDiscoverRow.FeaturedPodcasts(id = "list-featured", title = "Featured", podcasts = listOf(discoverPodcast("podcast-1")))

        createViewModel().playLatestEpisode(row, discoverPodcast("podcast-1"))

        verify(eventHorizon).track(
            DiscoverListEpisodeTappedEvent(listId = "list-featured", podcastUuid = "podcast-1", episodeUuid = "episode-new", source = "search"),
        )
        verify(eventHorizon).track(DiscoverListEpisodePlayEvent(listId = "list-featured", podcastUuid = "podcast-1"))
    }

    private fun podcastEpisode(uuid: String) = PodcastEpisode(uuid = uuid, publishedDate = Date(0))

    private fun discoverEpisode(uuid: String) = TvDiscoverEpisode(
        episodeUuid = uuid,
        episodeTitle = "Episode $uuid",
        podcastUuid = "podcast-$uuid",
        podcastTitle = "Podcast",
    )

    private fun podcastItem(uuid: String) = ImprovedSearchResultItem.PodcastItem(
        uuid = uuid,
        title = "Podcast $uuid",
        author = "Author",
        isFollowed = false,
    )

    private fun episodeItem(uuid: String) = ImprovedSearchResultItem.EpisodeItem(
        uuid = uuid,
        title = "Episode $uuid",
        podcastUuid = "podcast-$uuid",
        podcastTitle = "Podcast",
        publishedDate = Date(0),
        duration = 120.seconds,
    )

    private fun subscribedPodcast(uuid: String) = Podcast(uuid = uuid, title = "Podcast $uuid", author = "Author")

    @Test
    fun `matching folders are surfaced for a signed-in user`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever { folderManager.getAll() }.thenReturn(listOf(folderEntity("Sugar Shows"), folderEntity("Comedy")))
        whenever { folderManager.findFolderPodcastsSorted(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("Sugar Shows"), state.folders.map { it.folder.name })
    }

    @Test
    fun `matching folders are sorted by name`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever { folderManager.getAll() }.thenReturn(listOf(folderEntity("Sugar Rush"), folderEntity("Sugar Beats")))
        whenever { folderManager.findFolderPodcastsSorted(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("Sugar Beats", "Sugar Rush"), state.folders.map { it.folder.name })
    }

    @Test
    fun `folders are not searched for a signed-out user`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(listOf(podcastItem("podcast-1")))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertTrue(state.folders.isEmpty())
        verifyBlocking(folderManager, never()) { getAll() }
    }

    @Test
    fun `a matching folder alone counts as a result`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever { folderManager.getAll() }.thenReturn(listOf(folderEntity("Sugar")))
        whenever { folderManager.findFolderPodcastsSorted(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertTrue(viewModel.searchState.value is TvSearchState.Results)
    }

    @Test
    fun `a folder search failure does not fail the whole search`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(listOf(podcastItem("podcast-1")))
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever { folderManager.getAll() }.thenThrow(RuntimeException("database unavailable"))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("podcast-1"), state.podcasts.map { it.uuid })
        assertTrue(state.folders.isEmpty())
    }

    @Test
    fun `hasFolderResults reflects whether the terminal search found folders`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever { folderManager.getAll() }.thenReturn(listOf(folderEntity("Sugar")))
        whenever { folderManager.findFolderPodcastsSorted(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()
        assertTrue(viewModel.hasFolderResults.value)

        viewModel.onQueryChange("")
        assertFalse(viewModel.hasFolderResults.value)
    }

    @Test
    fun `a terminal search without folders resets the folders filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(listOf(podcastItem("podcast-1")))
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever { folderManager.getAll() }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onFilterSelected(TvSearchFilter.Folders)
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(TvSearchFilter.TopResults, viewModel.filter.value)
        assertFalse(viewModel.hasFolderResults.value)
    }

    @Test
    fun `a failed search clears folder results and resets the folders filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenThrow(RuntimeException("network"))
        whenever(syncManager.isLoggedIn()).thenReturn(true)

        val viewModel = createViewModel()
        viewModel.onFilterSelected(TvSearchFilter.Folders)
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertTrue(viewModel.searchState.value is TvSearchState.Error)
        assertEquals(TvSearchFilter.TopResults, viewModel.filter.value)
        assertFalse(viewModel.hasFolderResults.value)
    }

    private fun discoverPodcast(uuid: String) = TvDiscoverPodcast(uuid = uuid, title = "Title", author = "Author", description = "Description")

    private fun createViewModel() = TvSearchViewModel(
        discoverFeedLoader = TvDiscoverFeedLoader(
            listRepository = listRepository,
            settings = settings,
            applicationScope = CoroutineScope(coroutineRule.testDispatcher),
            context = context,
        ),
        syncManager = syncManager,
        improvedSearchManager = improvedSearchManager,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playbackManager = playbackManager,
        searchHistoryManager = searchHistoryManager,
        folderManager = folderManager,
        eventHorizon = eventHorizon,
        settings = settings,
    )

    private fun folderEntity(name: String) = Folder(
        uuid = name,
        name = name,
        color = 0,
        addedDate = Date(0),
        sortPosition = 0,
        podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
        deleted = false,
        syncModified = 0,
    )

    private fun category(id: Int, name: String) = DiscoverCategory(id = id, name = name, icon = "", source = "")

    private fun discover(vararg rows: DiscoverRow) = Discover(
        layout = rows.toList(),
        regions = mapOf("us" to DiscoverRegion(name = "United States", flag = "flag", code = "us")),
        regionCodeToken = "[regionCode]",
        regionNameToken = "[regionName]",
        defaultRegionCode = "us",
    )

    private fun row(
        id: String,
        title: String,
        source: String,
        type: ListType = ListType.PodcastList,
        displayStyle: DisplayStyle = DisplayStyle.SmallList(),
        authenticated: Boolean = false,
    ) = DiscoverRow(
        id = id,
        type = type,
        displayStyle = displayStyle,
        expandedStyle = ExpandedStyle.PlainList(),
        expandedTopItemLabel = null,
        title = title,
        source = source,
        listUuid = id,
        categoryId = null,
        regions = listOf("us"),
        curated = false,
        sponsored = false,
        authenticated = authenticated,
        mostPopularCategoriesId = null,
        sponsoredCategoryIds = null,
    )

    private fun categoriesRow(source: String) = row(
        id = "categories",
        title = "Browse By Category",
        source = source,
        type = ListType.Categories,
    )

    private fun bannerRow(id: String) = row(
        id = id,
        title = "",
        source = "",
        type = ListType.Unknown("banner"),
        displayStyle = DisplayStyle.Unknown("inline_banner"),
    )

    private fun podcastFeed(vararg podcastUuids: String) = ListFeed(
        title = null,
        subtitle = null,
        description = null,
        shortDescription = null,
        date = null,
        podcasts = podcastUuids.map { uuid ->
            DiscoverPodcast(
                uuid = uuid,
                title = "Podcast $uuid",
                url = null,
                author = null,
                category = null,
                description = null,
                language = null,
                mediaType = null,
            )
        },
        episodes = null,
        podroll = null,
        collectionImageUrl = null,
        collectionRectangleImageUrl = null,
        featureImage = null,
        headerImageUrl = null,
        tintColors = null,
        collageImages = null,
        webLinkUrl = null,
        webLinkTitle = null,
        promotion = null,
    )
}
