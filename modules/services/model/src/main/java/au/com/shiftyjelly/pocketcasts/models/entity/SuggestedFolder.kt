package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "suggested_folders",
    primaryKeys = ["folder_name", "podcast_uuid"],
)
data class SuggestedFolder(
    @ColumnInfo(name = "folder_name") var name: String,
    @ColumnInfo(name = "podcast_uuid") var podcastUuid: String,
)
