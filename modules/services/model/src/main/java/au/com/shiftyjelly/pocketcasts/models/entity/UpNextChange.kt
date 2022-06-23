package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "up_next_changes")
data class UpNextChange(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") var id: Long? = null,
    @ColumnInfo(name = "type") var type: Int = 0,
    @ColumnInfo(name = "uuid") var uuid: String? = null,
    @ColumnInfo(name = "uuids") var uuids: String? = null,
    @ColumnInfo(name = "modified") var modified: Long = 0
) : Serializable {

    companion object {
        const val ACTION_PLAY_NOW = 1
        const val ACTION_PLAY_NEXT = 2
        const val ACTION_PLAY_LAST = 3
        const val ACTION_REMOVE = 4
        const val ACTION_REPLACE = 5
    }
}
