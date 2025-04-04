package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.type.NotificationCategory

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["category", "subcategory"], unique = true),
    ],
)
data class Notifications(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") var id: Long? = null,
    @ColumnInfo(name = "category") var category: NotificationCategory,
    @ColumnInfo(name = "subcategory") val subcategory: String,
)

sealed class NotificationSubcategory(val value: String) {

    object Sync : NotificationSubcategory("sync")
    object Import : NotificationSubcategory("import")
    object UpNext : NotificationSubcategory("up_next")
    object Filters : NotificationSubcategory("filters")
    object Themes : NotificationSubcategory("themes")
    object StaffPicks : NotificationSubcategory("staff_picks")
    object PlusUpsell : NotificationSubcategory("plus_upsell")

    data class Feature(val featureKey: String) : NotificationSubcategory(featureKey)

    override fun toString(): String = value

    companion object {
        fun from(value: String): NotificationSubcategory = when (value) {
            "sync" -> Sync
            "import" -> Import
            "up_next" -> UpNext
            "filters" -> Filters
            "themes" -> Themes
            "staff_picks" -> StaffPicks
            "plus_upsell" -> PlusUpsell
            else -> Feature(value)
        }
    }
}
