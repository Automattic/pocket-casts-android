package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story

interface EndOfYearManager {
    suspend fun isEligibleForStories(): Boolean
    suspend fun downloadListeningHistory(onProgressChanged: (Float) -> Unit)
    suspend fun loadStories(): List<Story>
    suspend fun hasEpisodesPlayedUpto(year: Int, playedUpToInSecs: Long): Boolean
    suspend fun getTotalListeningTimeInSecsForYear(year: Int): Long?
    suspend fun findListenedCategoriesForYear(year: Int): List<ListenedCategory>
    suspend fun findListenedNumbersForYear(year: Int): ListenedNumbers
    suspend fun findTopPodcastsForYear(year: Int, limit: Int): List<TopPodcast>
    suspend fun findLongestPlayedEpisodeForYear(year: Int): LongestEpisode?
}
