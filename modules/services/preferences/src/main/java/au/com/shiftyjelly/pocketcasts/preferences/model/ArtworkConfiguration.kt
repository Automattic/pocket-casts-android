package au.com.shiftyjelly.pocketcasts.preferences.model

data class ArtworkConfiguration(
    val useEpisodeArtwork: Boolean,
    internal val enabledElements: Set<Element> = Element.entries.toSet(),
) {
    fun useEpisodeArtwork(element: Element) = useEpisodeArtwork && element in enabledElements

    fun enable(element: Element) = copy(enabledElements = enabledElements + element)

    fun disable(element: Element) = copy(enabledElements = enabledElements - element)

    enum class Element(
        internal val key: String,
    ) {
        Filters("filters"),
        UpNext("up_next"),
        Downloads("downloads"),
        Files("files"),
        Starred("starred"),
        Bookmarks("bookmarks"),
        ListeningHistory("listening_history"),
        ;

        companion object {
            internal fun fromKey(key: String) = entries.find { it.key == key }
        }
    }
}
