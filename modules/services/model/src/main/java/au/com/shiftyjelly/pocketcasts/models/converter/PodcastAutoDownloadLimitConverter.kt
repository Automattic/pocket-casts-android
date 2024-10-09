package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting

class PodcastAutoDownloadLimitConverter {

    @TypeConverter
    fun toInt(autoDownloadLimitSetting: AutoDownloadLimitSetting): Int = autoDownloadLimitSetting.id

    @TypeConverter
    fun toAutoDownloadLimitSetting(id: Int): AutoDownloadLimitSetting? = AutoDownloadLimitSetting.fromInt(id)
}
