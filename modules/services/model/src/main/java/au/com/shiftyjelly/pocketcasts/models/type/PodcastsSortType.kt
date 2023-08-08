package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.util.Locale

private val replaceTheRegex = "^the ".toRegex(RegexOption.IGNORE_CASE)
private fun cleanStringForSortInternal(value: String): String {
    return value.lowercase(Locale.getDefault()).replaceFirst(replaceTheRegex, "")
}

enum class PodcastsSortType(
    val clientId: Int,
    val serverId: Int,
    val labelId: Int,
    val podcastComparator: Comparator<Podcast>,
    val folderComparator: Comparator<FolderItem>,
    val analyticsValue: String,
) {
    DATE_ADDED_OLDEST_TO_NEWEST(
        clientId = 0,
        serverId = 0,
        labelId = R.string.podcasts_sort_by_date_added,
        podcastComparator = compareBy { it.addedDate },
        folderComparator = compareBy { it.addedDate },
        analyticsValue = "date_added"
    ),
    NAME_A_TO_Z(
        clientId = 2,
        serverId = 1,
        labelId = R.string.name,
        podcastComparator = compareBy { cleanStringForSortInternal(it.title) },
        folderComparator = compareBy { cleanStringForSortInternal(it.title) },
        analyticsValue = "name"
    ),
    EPISODE_DATE_NEWEST_TO_OLDEST(
        clientId = 5,
        serverId = 2,
        labelId = R.string.podcasts_sort_by_release_date,
        // use a query to get the podcasts ordered by episode release date
        podcastComparator = Comparator { _, _ -> 0 },
        folderComparator = Comparator { _, _ -> 0 },
        analyticsValue = "episode_release_date"
    ),
    DRAG_DROP(
        clientId = 6,
        serverId = 3,
        labelId = R.string.podcasts_sort_by_drag_drop,
        podcastComparator = compareBy { it.sortPosition },
        folderComparator = compareBy { it.sortPosition },
        analyticsValue = "drag_and_drop"
    );

    companion object {
        val default = DATE_ADDED_OLDEST_TO_NEWEST

        fun fromServerId(serverId: Int?): PodcastsSortType {
            val sortType = values().firstOrNull { it.serverId == serverId }
            if (sortType == null) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Invalid server ${PodcastsSortType::class.java.simpleName}: $serverId")
                return default
            }
            return sortType
        }

        fun fromClientIdString(clientIdString: String): PodcastsSortType {
            val clientId = clientIdString.toIntOrNull()
            val sortType = PodcastsSortType.values().firstOrNull { it.clientId == clientId }
            if (sortType == null) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Invalid client ${PodcastsSortType::class.java.simpleName}: $clientIdString")
                return default
            }
            return sortType
        }

        fun cleanStringForSort(value: String): String {
            return cleanStringForSortInternal(value)
        }
    }

    fun isAsc(): Boolean {
        return clientId == DATE_ADDED_OLDEST_TO_NEWEST.clientId || clientId == NAME_A_TO_Z.clientId
    }
}

class PodcastsSortTypeMoshiAdapter : JsonAdapter<PodcastsSortType>() {
    override fun toJson(writer: JsonWriter, value: PodcastsSortType?) {}

    override fun fromJson(reader: JsonReader): PodcastsSortType {
        return PodcastsSortType.fromServerId(reader.nextInt())
    }
}
