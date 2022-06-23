package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class FilePostBody(
    @field:Json(name = "files") val files: List<FilePost>
)

@JsonClass(generateAdapter = true)
data class FilePost(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "colour") val colour: Int,
    @field:Json(name = "playedUpTo") val playedUpTo: Int,
    @field:Json(name = "playingStatus") val playingStatus: Int,
    @field:Json(name = "duration") val duration: Int,
    @field:Json(name = "hasCustomImage") val hasCustomImage: Boolean
)

@JsonClass(generateAdapter = true)
data class FileUploadData(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "colour") val colour: Int,
    @field:Json(name = "contentType") val contentType: String,
    @field:Json(name = "duration") val duration: Int
)

@JsonClass(generateAdapter = true)
data class FileImageUploadData(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "size") val size: Long,
    @field:Json(name = "contentType") val contentType: String
)

@JsonClass(generateAdapter = true)
data class ServerFile(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "colour") val colour: Int,
    @field:Json(name = "contentType") val contentType: String,
    @field:Json(name = "duration") val duration: Int,
    @field:Json(name = "hasCustomImage") val hasCustomImage: Boolean,
    @field:Json(name = "imageUrl") val imageUrl: String,
    @field:Json(name = "playedUpTo") val playedUpTo: Int,
    @field:Json(name = "playedUpToModified") val playedUpToModified: Long,
    @field:Json(name = "playingStatus") val playingStatus: EpisodePlayingStatus,
    @field:Json(name = "playingStatusModified") val playingStatusModified: Long,
    @field:Json(name = "published") val publishedDate: Date,
    @field:Json(name = "size") val size: Long,
    @field:Json(name = "title") val title: String
)

@JsonClass(generateAdapter = true)
data class FileAccount(
    @field:Json(name = "totalFiles") val totalFiles: Int,
    @field:Json(name = "totalSize") val totalSize: Long,
    @field:Json(name = "usedSize") val usedSize: Long
)

@JsonClass(generateAdapter = true)
data class FilesResponse(
    @field:Json(name = "files") val files: List<ServerFile>,
    @field:Json(name = "account") val account: FileAccount
)

@JsonClass(generateAdapter = true)
data class FileUploadResponse(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class FileUrlResponse(
    @field:Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class FileUploadStatusResponse(
    @field:Json(name = "success") val success: Boolean
)
