package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class FolderResponse(
    @field:Json(name = "folderUuid") val folderUuid: String?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "color") val color: Int?,
    @field:Json(name = "sortPosition") val sortPosition: Int?,
    @field:Json(name = "podcastsSortType") val podcastsSortType: PodcastsSortType?,
    @field:Json(name = "dateAdded") val dateAdded: Date?
) {
    fun toFolder(): Folder? {
        if (folderUuid == null || name == null || color == null || sortPosition == null || podcastsSortType == null || dateAdded == null) {
            return null
        }
        return Folder(
            uuid = folderUuid,
            name = name,
            color = color,
            addedDate = dateAdded,
            sortPosition = sortPosition,
            podcastsSortType = podcastsSortType,
            deleted = false,
            syncModified = 0
        )
    }
}
