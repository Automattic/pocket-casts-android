package au.com.shiftyjelly.pocketcasts.models.db.helper

import androidx.room.Embedded
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import java.util.Date

data class PodcastBookmark(
    @Embedded var bookmark: Bookmark,
    val episodeTitle: String = "",
    val publishedDate: Date? = null,
) {
    fun toBookmark() = bookmark.copy(episodeTitle = episodeTitle)
}
