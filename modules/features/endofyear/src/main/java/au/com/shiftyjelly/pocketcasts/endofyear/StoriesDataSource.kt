package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import kotlinx.coroutines.flow.Flow

interface StoriesDataSource {
    suspend fun isEligibleForStories(): Flow<Boolean>
    suspend fun hasFullListeningHistory(): Flow<Boolean>
    suspend fun syncListeningHistory(): Flow<Boolean>
    suspend fun loadStories(): Flow<List<Story>>
}
