package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType

class PlaylistEpisodeSortTypeConverter {
    @TypeConverter
    fun toSortType(value: Int): PlaylistEpisodeSortType {
        return PlaylistEpisodeSortType.fromServerId(value) ?: PlaylistEpisodeSortType.NewestToOldest
    }

    @TypeConverter
    fun toString(value: PlaylistEpisodeSortType): Int {
        return value.serverId
    }
}
