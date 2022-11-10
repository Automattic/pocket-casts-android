package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import kotlinx.coroutines.flow.Flow

interface StoriesDataSource {
    fun isEligibleForStories(): Flow<Boolean>
    fun hasFullListeningHistory(): Flow<Boolean>
    suspend fun syncListeningHistory()
    fun loadStories(): Flow<List<Story>>
}
