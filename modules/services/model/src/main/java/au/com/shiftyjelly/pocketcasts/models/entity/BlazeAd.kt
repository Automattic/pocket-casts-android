package au.com.shiftyjelly.pocketcasts.models.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "blaze_ads")
data class BlazeAd(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "text") var text: String,
    @ColumnInfo(name = "image_url") var imageUrl: String,
    @ColumnInfo(name = "url_title") var urlTitle: String,
    @ColumnInfo(name = "url") var url: String,
    @ColumnInfo(name = "location") var location: BlazeAdLocation,
) : Parcelable
