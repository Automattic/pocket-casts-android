package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trending_podcasts")
data class TrendingPodcast(
    @PrimaryKey @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
)
