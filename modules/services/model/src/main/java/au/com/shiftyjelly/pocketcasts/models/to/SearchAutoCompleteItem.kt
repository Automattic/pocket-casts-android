package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date

sealed class SearchAutoCompleteItem {
    data class Term(val term: String) : SearchAutoCompleteItem()
    data class Podcast(
        val uuid: String,
        val title: String,
        val author: String,
        val isSubscribed: Boolean = false,
    ) : SearchAutoCompleteItem()
    data class Folder(
        val uuid: String,
        val title: String,
        val podcasts: List<Podcast>,
        val color: Int,
    ) : SearchAutoCompleteItem()
    data class Episode(
        val uuid: String,
        val title: String,
        val duration: Double,
        val publishedAt: Date,
        val podcastUuid: String,
    ) : SearchAutoCompleteItem()
}
