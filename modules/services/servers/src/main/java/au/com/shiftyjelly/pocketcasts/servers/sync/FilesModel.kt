package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class FilePostBody(
    @Json(name = "files") val files: List<FilePost>,
)

@JsonClass(generateAdapter = true)
data class FilePost(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String,
    @Json(name = "colour") val colour: Int,
    @Json(name = "playedUpTo") val playedUpTo: Int,
    @Json(name = "playingStatus") val playingStatus: Int,
    @Json(name = "duration") val duration: Int,
    @Json(name = "hasCustomImage") val hasCustomImage: Boolean,
)

@JsonClass(generateAdapter = true)
data class FileUploadData(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String,
    @Json(name = "colour") val colour: Int,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "duration") val duration: Int,
)

@JsonClass(generateAdapter = true)
data class FileImageUploadData(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "size") val size: Long,
    @Json(name = "contentType") val contentType: String,
)

@JsonClass(generateAdapter = true)
data class ServerFile(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "colour") val colour: Int,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "duration") val duration: Int,
    @Json(name = "hasCustomImage") val hasCustomImage: Boolean,
    @Json(name = "imageUrl") val imageUrl: String,
    @Json(name = "playedUpTo") val playedUpTo: Int,
    @Json(name = "playedUpToModified") val playedUpToModified: Long,
    @Json(name = "playingStatus") val playingStatus: EpisodePlayingStatus,
    @Json(name = "playingStatusModified") val playingStatusModified: Long,
    @Json(name = "published") val publishedDate: Date,
    @Json(name = "size") val size: Long,
    @Json(name = "title") val title: String,
)

@JsonClass(generateAdapter = true)
data class FileAccount(
    @Json(name = "totalFiles") val totalFiles: Int,
    @Json(name = "totalSize") val totalSize: Long,
    @Json(name = "usedSize") val usedSize: Long,
)

@JsonClass(generateAdapter = true)
data class FilesResponse(
    @Json(name = "files") val files: List<ServerFile>,
    @Json(name = "account") val account: FileAccount,
)

@JsonClass(generateAdapter = true)
data class FileUploadResponse(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "url") val url: String,
)

@JsonClass(generateAdapter = true)
data class FileUrlResponse(
    @Json(name = "url") val url: String,
)

@JsonClass(generateAdapter = true)
data class FileUploadStatusResponse(
    @Json(name = "success") val success: Boolean,
)
