package au.com.shiftyjelly.pocketcasts.servers.sync.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.pocketcasts.service.api.BookmarkResponse
import java.util.Date

fun BookmarkResponse.toBookmark(): Bookmark {
    return Bookmark(
        uuid = bookmarkUuid,
        podcastUuid = podcastUuid,
        episodeUuid = episodeUuid,
        timeSecs = time,
        createdAt = createdAt?.parseIsoDate() ?: Date(), // Date(Timestamps.toMillis(createdAt)),
        title = title
    )
}
