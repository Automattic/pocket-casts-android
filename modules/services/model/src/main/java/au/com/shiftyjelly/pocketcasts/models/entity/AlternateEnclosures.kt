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

/** The first HLS enclosure's MIME type, or null if none. Lets HLS-only episodes be detected synchronously. */
fun List<EpisodeAlternateEnclosure>?.firstHlsMimeType(): String? = this?.firstOrNull { BaseEpisode.isHlsMimeType(it.type) }?.type

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
