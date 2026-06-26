package au.com.shiftyjelly.pocketcasts.models.entity

/** First HLS enclosure's first http(s) source URI, or null if none can be streamed. */
fun List<EpisodeAlternateEnclosure>?.firstHlsStreamUrl(): String? {
    val enclosures = this ?: return null
    return enclosures
        .firstOrNull { BaseEpisode.isHlsMimeType(it.type) }
        ?.sources
        ?.firstOrNull { it.uri.isPlayableHttpUri() }
        ?.uri
}

/**
 * The stream to default to when the user hasn't picked one: the first HLS enclosure when HLS
 * streaming is on, else null so the caller falls back to the progressive download.
 */
fun List<EpisodeAlternateEnclosure>?.defaultHlsStreamUrl(hlsStreamingEnabled: Boolean): String? = if (hlsStreamingEnabled) firstHlsStreamUrl() else null

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
