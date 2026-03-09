package au.com.shiftyjelly.pocketcasts.preferences.model

import com.automattic.eventhorizon.EpisodeArtworkElementType

data class ArtworkConfiguration(
    val useEpisodeArtwork: Boolean,
    internal val enabledElements: Set<Element> = Element.entries.toSet(),
) {
    fun useEpisodeArtwork(element: Element) = useEpisodeArtwork && element in enabledElements

    fun enable(element: Element) = copy(enabledElements = enabledElements + element)

    fun disable(element: Element) = copy(enabledElements = enabledElements - element)

    enum class Element(
        internal val key: String,
        val eventHorizonValue: EpisodeArtworkElementType,
    ) {
        Filters(
            key = "filters",
            eventHorizonValue = EpisodeArtworkElementType.Filters,
        ),
        UpNext(
            key = "up_next",
            eventHorizonValue = EpisodeArtworkElementType.Upnext,
        ),
        Downloads(
            key = "downloads",
            eventHorizonValue = EpisodeArtworkElementType.Downloads,
        ),
        Files(
            key = "files",
            eventHorizonValue = EpisodeArtworkElementType.Files,
        ),
        Starred(
            key = "starred",
            eventHorizonValue = EpisodeArtworkElementType.Starred,
        ),
        Bookmarks(
            key = "bookmarks",
            eventHorizonValue = EpisodeArtworkElementType.Bookmarks,
        ),
        ListeningHistory(
            key = "listening_history",
            eventHorizonValue = EpisodeArtworkElementType.Listeninghistory,
        ),
        Podcasts(
            key = "podcasts",
            eventHorizonValue = EpisodeArtworkElementType.Podcasts,
        ),
        ;

        companion object {
            internal fun fromKey(key: String) = entries.find { it.key == key }
        }
    }
}
