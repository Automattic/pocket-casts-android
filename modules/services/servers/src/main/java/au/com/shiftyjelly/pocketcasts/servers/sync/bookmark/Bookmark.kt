package au.com.shiftyjelly.pocketcasts.servers.sync.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import com.pocketcasts.service.api.BookmarkResponse
import java.util.Date

fun BookmarkResponse.toBookmark(): Bookmark {
    return Bookmark(
        uuid = bookmarkUuid,
        podcastUuid = podcastUuid,
        episodeUuid = episodeUuid,
        timeSecs = time,
        createdAt = createdAt.toDate() ?: Date(),
        title = title
    )
}
