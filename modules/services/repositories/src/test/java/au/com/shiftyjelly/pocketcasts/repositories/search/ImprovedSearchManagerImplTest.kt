package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheService
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResponse
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResult
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteSearchService
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedResult
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedSearchResponse
import au.com.shiftyjelly.pocketcasts.servers.search.PodcastResultValue
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ImprovedSearchManagerImplTest {
    private val autoCompleteSearchService = mock<AutoCompleteSearchService>()
    private val combinedSearchService = mock<PodcastCacheService>()
    private val manager = ImprovedSearchManagerImpl(
        autoCompleteSearchService = autoCompleteSearchService,
        combinedSearchService = combinedSearchService,
    )

    @Test
    fun `combined search drops podcasts with a null title and keeps the rest`() = runTest {
        whenever(combinedSearchService.combinedSearch(any())) doReturn CombinedSearchResponse(
            results = listOf(
                CombinedResult.EpisodeResult(
                    uuid = "episode-uuid",
                    title = "Big Sugar",
                    publishedDate = Date(0),
                    url = "https://example.com/audio.mp3",
                    duration = 1114,
                    podcastUuid = "podcast-uuid",
                    podcastTitle = "Business Daily",
                    podcastSlug = "business-daily",
                ),
                CombinedResult.PodcastResult(
                    uuid = "podcast-uuid",
                    title = "Big Sugar",
                    author = "Weekday Fun Productions",
                    slug = "big-sugar",
                    explicit = false,
                ),
                // The offending result from the bug report: a podcast with a null title.
                CombinedResult.PodcastResult(
                    uuid = "null-title-uuid",
                    title = null,
                    author = "WGTE Public Media",
                    slug = "untitled",
                    explicit = false,
                ),
            ),
        )

        val results = manager.combinedSearch("big sugar")

        assertEquals(
            listOf("episode-uuid", "podcast-uuid"),
            results.map { it.uuid },
        )
        assertTrue(results.none { it is ImprovedSearchResultItem.PodcastItem && it.uuid == "null-title-uuid" })

        val episode = results.filterIsInstance<ImprovedSearchResultItem.EpisodeItem>().single()
        assertEquals("Business Daily", episode.podcastTitle)
    }

    @Test
    fun `combined search drops unknown result types`() = runTest {
        whenever(combinedSearchService.combinedSearch(any())) doReturn CombinedSearchResponse(
            results = listOf(
                CombinedResult.PodcastResult(
                    uuid = "podcast-uuid",
                    title = "Big Sugar",
                    author = "Weekday Fun Productions",
                    slug = "big-sugar",
                    explicit = false,
                ),
                CombinedResult.Unknown,
            ),
        )

        val results = manager.combinedSearch("big sugar")

        assertEquals(listOf("podcast-uuid"), results.map { it.uuid })
    }

    @Test
    fun `autocomplete search drops unknown result types`() = runTest {
        whenever(autoCompleteSearchService.autoCompleteSearch(any(), anyOrNull(), anyOrNull())) doReturn AutoCompleteResponse(
            results = listOf(
                AutoCompleteResult.TermResult(value = "big sugar"),
                AutoCompleteResult.PodcastResult(
                    value = PodcastResultValue(uuid = "podcast-uuid", title = "Big Sugar"),
                ),
                AutoCompleteResult.Unknown,
            ),
        )

        val results = manager.autoCompleteSearch("big sugar")

        assertEquals(
            listOf<SearchAutoCompleteItem>(
                SearchAutoCompleteItem.Term(term = "big sugar"),
                SearchAutoCompleteItem.Podcast(uuid = "podcast-uuid", title = "Big Sugar", author = "", isExplicit = false),
            ),
            results,
        )
    }
}
