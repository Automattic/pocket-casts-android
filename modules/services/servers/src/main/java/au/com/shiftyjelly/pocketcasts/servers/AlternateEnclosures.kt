package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure

/** First HLS enclosure's first http(s) source URI, or null if none can be streamed. */
internal fun List<EpisodeAlternateEnclosure>?.firstHlsStreamUrl(): String? {
    val enclosures = this ?: return null
    return enclosures
        .firstOrNull { BaseEpisode.isHlsMimeType(it.type) }
        ?.sources
        ?.firstOrNull { it.uri.isPlayableHttpUri() }
        ?.uri
}

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
