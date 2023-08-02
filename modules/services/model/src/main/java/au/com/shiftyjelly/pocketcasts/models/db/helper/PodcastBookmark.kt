package au.com.shiftyjelly.pocketcasts.models.db.helper

import androidx.room.Embedded
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark

data class PodcastBookmark(
    @Embedded var bookmark: Bookmark,
    val episodeTitle: String = "",
) {
    fun toBookmark() = bookmark.copy(episodeTitle = episodeTitle)
}
