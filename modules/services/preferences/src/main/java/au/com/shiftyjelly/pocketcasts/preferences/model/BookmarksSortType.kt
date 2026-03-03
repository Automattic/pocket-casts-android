package au.com.shiftyjelly.pocketcasts.preferences.model

import com.automattic.eventhorizon.BookmarkSortType

interface BookmarksSortType {
    val labelId: Int
    val key: String
    val eventHorizonValue: BookmarkSortType
}
