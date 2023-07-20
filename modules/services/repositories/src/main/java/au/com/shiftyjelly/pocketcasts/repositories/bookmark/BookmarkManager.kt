package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import kotlinx.coroutines.flow.Flow

interface BookmarkManager {
    suspend fun add(episode: BaseEpisode, timeSecs: Int, title: String): Bookmark
    suspend fun findBookmark(bookmarkUuid: String): Bookmark?
    suspend fun findEpisodeBookmarksFlow(
        episode: BaseEpisode,
        sortType: BookmarksSortType,
        isAsc: Boolean = false,
    ): Flow<List<Bookmark>>

    suspend fun deleteToSync(bookmarkUuid: String)
    suspend fun deleteSynced(bookmarkUuid: String)
    suspend fun upsertSynced(bookmark: Bookmark): Bookmark
    fun findBookmarksToSync(): List<Bookmark>
}
