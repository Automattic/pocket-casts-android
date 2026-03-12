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
        val analyticsValue: EpisodeArtworkElementType,
    ) {
        Filters(
            key = "filters",
            analyticsValue = EpisodeArtworkElementType.Filters,
        ),
        UpNext(
            key = "up_next",
            analyticsValue = EpisodeArtworkElementType.Upnext,
        ),
        Downloads(
            key = "downloads",
            analyticsValue = EpisodeArtworkElementType.Downloads,
        ),
        Files(
            key = "files",
            analyticsValue = EpisodeArtworkElementType.Files,
        ),
        Starred(
            key = "starred",
            analyticsValue = EpisodeArtworkElementType.Starred,
        ),
        Bookmarks(
            key = "bookmarks",
            analyticsValue = EpisodeArtworkElementType.Bookmarks,
        ),
        ListeningHistory(
            key = "listening_history",
            analyticsValue = EpisodeArtworkElementType.Listeninghistory,
        ),
        Podcasts(
            key = "podcasts",
            analyticsValue = EpisodeArtworkElementType.Podcasts,
        ),
        ;

        companion object {
            internal fun fromKey(key: String) = entries.find { it.key == key }
        }
    }
}
