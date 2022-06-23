package au.com.shiftyjelly.pocketcasts.servers.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

sealed class ExpandedStyle(val stringValue: String) {
    companion object {
        private const val RANKED_LIST = "ranked_list"
        private const val PLAIN_LIST = "plain_list"
        private const val DESCRIPTIVE_LIST = "descriptive_list"
        private const val GRID_LIST = "grid"

        fun fromString(value: String?): ExpandedStyle? {
            return when (value) {
                RANKED_LIST -> RankedList()
                PLAIN_LIST -> PlainList()
                DESCRIPTIVE_LIST -> DescriptiveList()
                GRID_LIST -> GridList()
                else -> PlainList()
            }
        }
    }

    class RankedList : ExpandedStyle(RANKED_LIST)
    class PlainList : ExpandedStyle(PLAIN_LIST)
    class DescriptiveList : ExpandedStyle(DESCRIPTIVE_LIST)
    class GridList : ExpandedStyle(GRID_LIST)
}

class ExpandedStyleMoshiAdapter {
    @FromJson fun fromJson(value: String): ExpandedStyle? {
        return ExpandedStyle.fromString(value)
    }

    @ToJson fun toJson(expandedStyle: ExpandedStyle): String {
        return expandedStyle.stringValue
    }
}
