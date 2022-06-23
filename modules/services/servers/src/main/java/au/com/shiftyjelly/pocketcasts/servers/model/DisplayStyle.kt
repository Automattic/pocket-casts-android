package au.com.shiftyjelly.pocketcasts.servers.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

sealed class DisplayStyle(val stringValue: String) {
    companion object {
        private const val CAROUSEL = "carousel"
        private const val SMALL_LIST = "small_list"
        private const val LARGE_LIST = "large_list"
        private const val NETWORK = "network"
        private const val CATEGORY = "category"
        private const val SINGLE_PODCAST = "single_podcast"
        private const val SINGLE_EPISODE = "single_episode"
        private const val COLLECTION_LIST = "collection"

        fun fromString(value: String): DisplayStyle? {
            return when (value) {
                CAROUSEL -> Carousel()
                SMALL_LIST -> SmallList()
                LARGE_LIST -> LargeList()
                NETWORK -> Network()
                CATEGORY -> Category()
                SINGLE_PODCAST -> SinglePodcast()
                SINGLE_EPISODE -> SingleEpisode()
                COLLECTION_LIST -> CollectionList()
                else -> Unknown(value)
            }
        }
    }

    class Carousel : DisplayStyle(CAROUSEL)
    class SmallList : DisplayStyle(SMALL_LIST)
    class LargeList : DisplayStyle(LARGE_LIST)
    class Network : DisplayStyle(NETWORK)
    class Category : DisplayStyle(CATEGORY)
    class SinglePodcast : DisplayStyle(SINGLE_PODCAST)
    class SingleEpisode : DisplayStyle(SINGLE_EPISODE)
    class CollectionList : DisplayStyle(COLLECTION_LIST)
    data class Unknown(val value: String) : DisplayStyle(value)

    override fun toString(): String {
        return stringValue
    }
}

class DisplayStyleMoshiAdapter {
    @FromJson fun fromJson(value: String): DisplayStyle? {
        return DisplayStyle.fromString(value)
    }

    @ToJson fun toJson(displayStyle: DisplayStyle): String {
        return displayStyle.stringValue
    }
}
