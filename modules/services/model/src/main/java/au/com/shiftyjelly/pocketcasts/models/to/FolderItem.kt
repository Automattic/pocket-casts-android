package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.models.entity.Folder as FolderModel
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastModel

sealed class FolderItem(
    val uuid: String,
    val title: String,
    val addedDate: Date,
    val sortPosition: Int,
    val subscribed: Boolean,
) {
    data class Podcast(val podcast: PodcastModel) :
        FolderItem(
            uuid = podcast.uuid,
            title = podcast.title,
            addedDate = podcast.addedDate ?: Date(Long.MIN_VALUE),
            sortPosition = podcast.sortPosition,
            subscribed = podcast.isSubscribed,
        )

    data class Folder(val folder: FolderModel, val podcasts: List<PodcastModel>) :
        FolderItem(
            uuid = folder.uuid,
            title = folder.name,
            addedDate = folder.addedDate,
            sortPosition = folder.sortPosition,
            subscribed = true,
        )

    val adapterId: Long
        get() = UUID.nameUUIDFromBytes(uuid.toByteArray()).mostSignificantBits
}
