package au.com.shiftyjelly.pocketcasts.utils

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import java.util.Date
import java.util.UUID

object FakeFileGenerator {
    val uuid = UUID.randomUUID().toString()
    val fakeFolder = Folder(
        uuid = uuid,
        name = "name",
        color = 1,
        addedDate = Date(),
        sortPosition = 1,
        podcastsSortType = PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST,
        deleted = false,
        syncModified = 1L
    )
}
