package au.com.shiftyjelly.pocketcasts.models.db.helper

import androidx.room.Embedded
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import java.util.Date

data class ProfileBookmark(
    @Embedded var bookmark: Bookmark,
    val episodeTitle: String? = null,
    val publishedDate: Date? = null,
    val podcastTitle: String? = null,
) {
    fun toBookmark() = bookmark.copy(
        episodeTitle = episodeTitle ?: "",
        podcastTitle = podcastTitle ?: "",
    )
}
