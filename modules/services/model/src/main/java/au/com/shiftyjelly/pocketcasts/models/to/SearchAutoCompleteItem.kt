package au.com.shiftyjelly.pocketcasts.models.to

sealed class SearchAutoCompleteItem {
    data class Term(val term: String) : SearchAutoCompleteItem()
    data class Podcast(
        val uuid: String,
        val title: String,
        val author: String,
        val isSubscribed: Boolean = false,
    ) : SearchAutoCompleteItem()
    data object Episode : SearchAutoCompleteItem()
}
