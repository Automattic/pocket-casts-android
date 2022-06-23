package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "filter_episodes",
    indices = [
        (Index(name = "filter_episodes_playlist_id", value = arrayOf("playlistId")))
    ]
)

data class PlaylistEpisode(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") var id: Long? = null,
    @ColumnInfo(name = "playlistId") var playlistId: Long,
    @ColumnInfo(name = "episodeUuid") var episodeUuid: String,
    @ColumnInfo(name = "position") var position: Int = 0
) : Serializable
