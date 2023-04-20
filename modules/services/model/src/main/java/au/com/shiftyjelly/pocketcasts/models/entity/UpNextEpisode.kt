package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(tableName = "up_next_episodes")
data class UpNextEpisode(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") var id: Long? = null,
    @ColumnInfo(name = "episodeUuid") var episodeUuid: String,
    @ColumnInfo(name = "position") var position: Int = 0,
    @ColumnInfo(name = "playlistId") var playlistId: Long? = null,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "publishedDate") var publishedDate: Date? = null,
    @ColumnInfo(name = "downloadUrl") var downloadUrl: String? = null,
    @ColumnInfo(name = "podcastUuid") var podcastUuid: String? = null
) : Serializable {

    fun toSimpleEpisode(): PodcastEpisode {
        return PodcastEpisode(
            uuid = episodeUuid,
            podcastUuid = podcastUuid ?: "",
            title = title,
            publishedDate = publishedDate ?: Date(),
            downloadUrl = downloadUrl
        )
    }
}

fun Episode.toUpNextEpisode(): UpNextEpisode {
    return UpNextEpisode(
        episodeUuid = uuid,
        podcastUuid = if (this is PodcastEpisode) podcastUuid else null,
        title = title,
        downloadUrl = downloadUrl,
        publishedDate = publishedDate
    )
}
