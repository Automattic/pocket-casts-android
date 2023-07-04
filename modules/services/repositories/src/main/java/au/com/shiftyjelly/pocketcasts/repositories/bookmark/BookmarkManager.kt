package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkManager {
    suspend fun add(episode: BaseEpisode, timeSecs: Int): Bookmark
    suspend fun findBookmark(bookmarkUuid: String): Bookmark?
    suspend fun findEpisodeBookmarks(episode: BaseEpisode): Flow<List<Bookmark>>
    suspend fun deleteToSync(bookmarkUuid: String)
    suspend fun deleteSynced(bookmarkUuid: String)
}
