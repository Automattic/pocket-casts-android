package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation

class BlazeAdLocationConverter {

    @TypeConverter
    fun toBlazeAdLocation(value: String): BlazeAdLocation {
        return BlazeAdLocation.fromServer(value)
    }

    @TypeConverter
    fun toString(blazeAdLocation: BlazeAdLocation): String {
        return blazeAdLocation.value
    }
}
