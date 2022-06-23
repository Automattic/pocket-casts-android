package au.com.shiftyjelly.pocketcasts.servers.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

sealed class ListType(val stringValue: String) {
    companion object {
        private const val PODCAST_LIST = "podcast_list"
        private const val EPISODE_LIST = "episode_list"
        private const val CATEGORIES = "categories"

        fun fromString(value: String): ListType {
            return when (value) {
                PODCAST_LIST -> PodcastList()
                EPISODE_LIST -> EpisodeList()
                CATEGORIES -> Categories()
                else -> Unknown(value)
            }
        }
    }

    class PodcastList : ListType(PODCAST_LIST)
    class EpisodeList : ListType(EPISODE_LIST)
    class Categories : ListType(CATEGORIES)
    data class Unknown(val value: String) : ListType(value)

    override fun toString(): String {
        return stringValue
    }
}

class ListTypeMoshiAdapter {
    @FromJson
    fun fromJson(value: String): ListType {
        return ListType.fromString(value)
    }

    @ToJson
    fun toJson(listType: ListType): String {
        return listType.stringValue
    }
}
