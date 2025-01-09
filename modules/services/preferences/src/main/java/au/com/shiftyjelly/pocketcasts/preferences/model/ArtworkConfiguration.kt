package au.com.shiftyjelly.pocketcasts.preferences.model

data class ArtworkConfiguration(
    val useEpisodeArtwork: Boolean,
    internal val enabledElements: Set<Element> = Element.entries.toSet(),
) {
    fun useEpisodeArtwork(element: Element) = useEpisodeArtwork && element in enabledElements

    fun enable(element: Element) = copy(enabledElements = enabledElements + element)

    fun disable(element: Element) = copy(enabledElements = enabledElements - element)

    enum class Element(
        val analyticsValue: String,
        internal val key: String,
    ) {
        Filters(key = "filters", analyticsValue = "filters"),
        UpNext(key = "up_next", analyticsValue = "upnext"),
        Downloads(key = "downloads", analyticsValue = "downloads"),
        Files(key = "files", analyticsValue = "files"),
        Starred(key = "starred", analyticsValue = "starred"),
        Bookmarks(key = "bookmarks", analyticsValue = "bookmarks"),
        ListeningHistory(key = "listening_history", analyticsValue = "listeninghistory"),
        Podcasts(key = "podcasts", analyticsValue = "podcasts"),
        ;

        companion object {
            internal fun fromKey(key: String) = entries.find { it.key == key }
        }
    }
}
