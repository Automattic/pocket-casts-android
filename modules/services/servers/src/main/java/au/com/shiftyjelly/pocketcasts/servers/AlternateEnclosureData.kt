package au.com.shiftyjelly.pocketcasts.servers

/** Podcasting 2.0 `<podcast:alternateEnclosure>`, normalized to the [type] and source URIs we read. */
internal class AlternateEnclosureData(
    val type: String?,
    val sourceUris: List<String>,
)

// HLS is advertised as either of these MIME types (matches BaseEpisode.isHlsMimeType).
private val HLS_MIME_TYPES = setOf("application/x-mpegurl", "application/vnd.apple.mpegurl")

/** First HLS enclosure's first http(s) source URI, or null if none can be streamed. */
internal fun List<AlternateEnclosureData>?.firstHlsStreamUrl(): String? {
    val enclosures = this ?: return null
    return enclosures
        .firstOrNull { it.type?.lowercase() in HLS_MIME_TYPES }
        ?.sourceUris
        ?.firstOrNull { it.isPlayableHttpUri() }
}

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
