package au.com.shiftyjelly.pocketcasts.servers.discover

interface SearchResult {

    val title: String
    val description: String
    val isAudio: Boolean
}
