package au.com.shiftyjelly.pocketcasts.engage

import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastView
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationsTest {
    @Test
    fun `create recommendations data`() {
        val data = Recommendations.create(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 10),
                "key-2" to createCuratedList("key-2", 5),
            ),
        )

        val expected = Recommendations(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            curatedRecommendations = listOf(
                createCuratedList("key-1", 10),
                createCuratedList("key-2", 5),
            ),
        )
        assertEquals(expected, data)
    }

    @Test
    fun `limit number of curated recommnedations`() {
        val data = Recommendations.create(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 10),
                "key-2" to createCuratedList("key-2", 5),
                "key-3" to createCuratedList("key-3", 1),
            ),
        )

        assertEquals(2, data.curatedRecommendations.size)
    }

    @Test
    fun `sort curated recommendations by list size`() {
        val data = Recommendations.create(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 2),
                "key-2" to createCuratedList("key-2", 8),
            ),
        )

        val expected = listOf(
            createCuratedList("key-2", 8),
            createCuratedList("key-1", 2),
        )
        assertEquals(expected, data.curatedRecommendations)
    }

    @Test
    fun `include more curated recommendations if recently played is empty`() {
        val data = Recommendations.create(
            recentlyPlayed = emptyList(),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 8),
                "key-2" to createCuratedList("key-2", 5),
                "key-3" to createCuratedList("key-3", 4),
                "key-4" to createCuratedList("key-4", 2),
            ),
        )

        val expected = listOf(
            createCuratedList("key-1", 8),
            createCuratedList("key-2", 5),
            createCuratedList("key-3", 4),
        )
        assertEquals(expected, data.curatedRecommendations)
    }

    @Test
    fun `include more curated recommendations if new releases are empty`() {
        val data = Recommendations.create(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = emptyList(),
            trending = createCuratedList("trending", 2),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 8),
                "key-2" to createCuratedList("key-2", 5),
                "key-3" to createCuratedList("key-3", 4),
                "key-4" to createCuratedList("key-4", 2),
            ),
        )

        val expected = listOf(
            createCuratedList("key-1", 8),
            createCuratedList("key-2", 5),
            createCuratedList("key-3", 4),
        )
        assertEquals(expected, data.curatedRecommendations)
    }

    @Test
    fun `include more curated recommendations if trending are empty`() {
        val data = Recommendations.create(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 0),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 8),
                "key-2" to createCuratedList("key-2", 5),
                "key-3" to createCuratedList("key-3", 4),
                "key-4" to createCuratedList("key-4", 2),
            ),
        )

        val expected = listOf(
            createCuratedList("key-1", 8),
            createCuratedList("key-2", 5),
            createCuratedList("key-3", 4),
        )
        assertEquals(expected, data.curatedRecommendations)
    }

    @Test
    fun `include more curated recommendations if they are the only present`() {
        val data = Recommendations.create(
            recentlyPlayed = emptyList(),
            newReleases = emptyList(),
            trending = createCuratedList("trending", 0),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 8),
                "key-2" to createCuratedList("key-2", 5),
                "key-3" to createCuratedList("key-3", 4),
                "key-4" to createCuratedList("key-4", 3),
                "key-5" to createCuratedList("key-5", 2),
                "key-6" to createCuratedList("key-6", 1),
            ),
        )

        val expected = listOf(
            createCuratedList("key-1", 8),
            createCuratedList("key-2", 5),
            createCuratedList("key-3", 4),
            createCuratedList("key-4", 3),
            createCuratedList("key-5", 2),
        )
        assertEquals(expected, data.curatedRecommendations)
    }

    @Test
    fun `do not include empty recommendations`() {
        val data = Recommendations.create(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            recommendations = mapOf(
                "key-1" to createCuratedList("key-1", 0),
                "key-2" to createCuratedList("key-2", 0),
            ),
        )

        val expected = Recommendations(
            recentlyPlayed = createRecentlyPlayed(5),
            newReleases = createNewReleases(3),
            trending = createCuratedList("trending", 2),
            curatedRecommendations = emptyList(),
        )
        assertEquals(expected, data)
    }

    private fun createRecentlyPlayed(size: Int) = List(size) { index ->
        ExternalPodcast(
            id = "id-$index",
            title = "title-$index",
            description = "description-$index",
            episodeCount = index,
            initialReleaseTimestampMs = index.toLong(),
            lastUsedTimestampMs = index.toLong(),
            latestReleaseTimestampMs = index.toLong(),
            _categories = "categories-$index",
        )
    }

    private fun createNewReleases(size: Int) = List(size) { index ->
        ExternalEpisode.Podcast(
            id = "id-$index",
            title = "title-$index",
            releaseTimestampMs = index.toLong(),
            durationMs = index.toLong(),
            playbackPositionMs = index.toLong(),
            isDownloaded = false,
            isVideo = false,
            lastUsedTimestampMs = index.toLong(),
            podcastTitle = "podcast-title_$index",
            podcastId = "podcast-id-$index",
            seasonNumber = index,
            episodeNumber = index,
        )
    }

    private fun createCuratedList(title: String, size: Int) = ExternalPodcastList(
        title = title,
        id = "id-$title",
        podcasts = List(size) { index ->
            ExternalPodcastView(
                id = "id-$index",
                title = "title-$index",
                description = "description-$index",
            )
        },
    )
}
