package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.MediaKind

/** A playable rendition resolved from an alternate enclosure. */
data class AlternateEnclosureStream(
    val url: String,
    val contentType: String?,
    val isHls: Boolean,
    val isVideo: Boolean,
)

/** First HLS enclosure's first http(s) source URI, or null if none can be streamed. */
fun List<EpisodeAlternateEnclosure>?.firstHlsStreamUrl(): String? = firstHlsStream()?.url

/** The first HLS enclosure's MIME type, or null if none. Lets HLS-only episodes be detected synchronously. */
fun List<EpisodeAlternateEnclosure>?.firstHlsMimeType(): String? = this?.firstOrNull { BaseEpisode.isHlsMimeType(it.type) }?.type

/** The streamable HLS rendition, preferred over [firstProgressiveVideoStream] because it adapts to bandwidth. */
fun List<EpisodeAlternateEnclosure>?.firstHlsStream(): AlternateEnclosureStream? = this
    ?.filter { BaseEpisode.isHlsMimeType(it.type) }
    ?.firstNotNullOfOrNull { it.toStream(isHls = true) }

/** The streamable non-HLS rendition the server marked `media_kind = "video"`, such as a `video/mp4` enclosure. */
fun List<EpisodeAlternateEnclosure>?.firstProgressiveVideoStream(): AlternateEnclosureStream? = this
    ?.filter { it.isProgressiveVideo }
    ?.firstNotNullOfOrNull { it.toStream(isHls = false) }

/** MIME type to record on an episode the server sent no progressive enclosure for. */
fun List<EpisodeAlternateEnclosure>?.firstStreamOnlyMimeType(): String? = firstHlsMimeType() ?: this?.firstOrNull { it.isProgressiveVideo }?.type

private val EpisodeAlternateEnclosure.isProgressiveVideo: Boolean
    get() = mediaKind == MediaKind.Video && !BaseEpisode.isHlsMimeType(type)

private fun EpisodeAlternateEnclosure.toStream(isHls: Boolean): AlternateEnclosureStream? {
    val source = sources.firstOrNull { it.uri.isPlayableHttpUri() } ?: return null
    // The enclosure's own type describes the rendition; a source may declare a generic type such as application/octet-stream.
    val contentType = type?.takeIf { it.isNotBlank() } ?: source.contentType?.takeIf { it.isNotBlank() }
    return AlternateEnclosureStream(url = source.uri, contentType = contentType, isHls = isHls, isVideo = mediaKind == MediaKind.Video)
}

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
