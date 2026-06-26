package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A Podcasting 2.0 `<podcast:alternateEnclosure>` as sent by the server, stored verbatim per episode. */
@Entity(
    tableName = "episode_alternate_enclosures",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEpisode::class,
            parentColumns = ["uuid"],
            childColumns = ["episode_uuid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(name = "episode_alternate_enclosure_episode_uuid_index", value = ["episode_uuid"]),
    ],
)
data class EpisodeAlternateEnclosure(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0,
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "bitrate") val bitrate: Long? = null,
    @ColumnInfo(name = "length") val length: Long? = null,
    @ColumnInfo(name = "height") val height: Int? = null,
    @ColumnInfo(name = "width") val width: Int? = null,
    @ColumnInfo(name = "lang") val lang: String? = null,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "codecs") val codecs: String? = null,
    @ColumnInfo(name = "integrity_type") val integrityType: String? = null,
    @ColumnInfo(name = "integrity_value") val integrityValue: String? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "sources") val sources: List<AlternateEnclosureSource> = emptyList(),
)

/** One `<source>` of an [EpisodeAlternateEnclosure]; multiple sources mirror the same rendition. */
@JsonClass(generateAdapter = true)
data class AlternateEnclosureSource(
    @Json(name = "uri") val uri: String,
    @Json(name = "content_type") val contentType: String? = null,
)
