package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure

// HLS is advertised as either of these MIME types (matches BaseEpisode.isHlsMimeType).
private val HLS_MIME_TYPES = setOf("application/x-mpegurl", "application/vnd.apple.mpegurl")

/** First HLS enclosure's first http(s) source URI, or null if none can be streamed. */
internal fun List<EpisodeAlternateEnclosure>?.firstHlsStreamUrl(): String? {
    val enclosures = this ?: return null
    return enclosures
        .firstOrNull { it.type?.lowercase() in HLS_MIME_TYPES }
        ?.sources
        ?.firstOrNull { it.uri.isPlayableHttpUri() }
        ?.uri
}

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
