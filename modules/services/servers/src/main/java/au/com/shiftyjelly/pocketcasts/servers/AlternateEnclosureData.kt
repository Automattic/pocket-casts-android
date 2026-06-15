package au.com.shiftyjelly.pocketcasts.servers

/**
 * Podcasting 2.0 `<podcast:alternateEnclosure>`, normalized to the only two pieces we read:
 * the advertised [type] and the candidate source URIs. Both the Moshi (podcast endpoint) and
 * org.json (cache/refresh/share) parsers map their raw shapes onto this so HLS selection stays
 * in one place.
 */
internal class AlternateEnclosureData(
    val type: String?,
    val sourceUris: List<String>,
)

// HLS is advertised as either of these MIME types (matches BaseEpisode.isHlsMimeType).
private val HLS_MIME_TYPES = setOf("application/x-mpegurl", "application/vnd.apple.mpegurl")

/**
 * Returns the HLS (m3u8) stream URL from a list of alternate enclosures, or null if none.
 * Picks the first enclosure advertised as HLS, then its first playable http(s) source —
 * ipfs/torrent/.onion transports are skipped since the player can't stream them.
 */
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
