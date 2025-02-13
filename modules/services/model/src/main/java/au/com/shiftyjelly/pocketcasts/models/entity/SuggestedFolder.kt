package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggested_folders")
data class SuggestedFolder(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "folder_name") var name: String,
    @ColumnInfo(name = "podcastUuid") var podcastUuid: String,
)
