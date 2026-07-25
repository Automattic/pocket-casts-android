package au.com.shiftyjelly.pocketcasts.models.to

enum class ChapterOrigin(val id: Int) {
    Unknown(0),
    PodcastIndex(1),
    ShowNotes(2),
    NativeMedia(3),
    Generated(4),
    ;

    companion object {
        fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: Unknown
    }
}
