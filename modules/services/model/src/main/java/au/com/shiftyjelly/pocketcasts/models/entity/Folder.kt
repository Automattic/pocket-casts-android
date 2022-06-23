package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import java.util.Date

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "color") var color: Int,
    @ColumnInfo(name = "added_date") var addedDate: Date,
    // position in the grid
    @ColumnInfo(name = "sort_position") var sortPosition: Int,
    // order of the podcasts in the folder. A to Z, episode release date, date added, drag and drop
    @ColumnInfo(name = "podcasts_sort_type") var podcastsSortType: PodcastsSortType,
    @ColumnInfo(name = "deleted") var deleted: Boolean,
    @ColumnInfo(name = "sync_modified") var syncModified: Long
) {

    companion object {
        // server side, the home folder also needs a UUID, so again we have a predefined value for it all clients use
        const val homeFolderUuid = "973df93c-e4dc-41fb-879e-0c7b532ebb70"
    }
}
