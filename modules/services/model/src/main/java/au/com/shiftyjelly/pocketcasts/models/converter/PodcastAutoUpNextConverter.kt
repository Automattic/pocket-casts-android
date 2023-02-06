package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

class PodcastAutoUpNextConverter {

    @TypeConverter
    fun toPodcastAutoUpNext(value: Int?) =
        Podcast.AutoAddUpNext.fromDatabaseInt(value)

    @TypeConverter
    fun toInt(value: Podcast.AutoAddUpNext?) =
        value?.databaseInt
}
