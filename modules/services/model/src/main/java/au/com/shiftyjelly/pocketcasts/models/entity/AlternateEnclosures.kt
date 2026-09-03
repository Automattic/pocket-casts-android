package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.MediaKind

data class AlternateEnclosureStream(
    val url: String,
    val contentType: String?,
    val mediaKind: MediaKind?,
) {
    val isHls: Boolean
        get() = BaseEpisode.isHlsMimeType(contentType) || BaseEpisode.isHlsUrl(url)

    val isVideo: Boolean
        get() = mediaKind == MediaKind.Video

    /** The server said audio explicitly, which is not the same as leaving the kind unstated. */
    val isAudioOnly: Boolean
        get() = mediaKind == MediaKind.Audio
}

/** First HLS enclosure's first http(s) source URI, or null if none can be streamed. */
fun List<EpisodeAlternateEnclosure>?.firstHlsStreamUrl(): String? = firstHlsStream()?.url

/** The first HLS enclosure's MIME type, or null if none. Lets HLS-only episodes be detected synchronously. */
fun List<EpisodeAlternateEnclosure>?.firstHlsMimeType(): String? = this?.firstOrNull { BaseEpisode.isHlsMimeType(it.type) }?.type

/** The streamable HLS encoding, which adapts to bandwidth where a progressive rendition cannot. */
fun List<EpisodeAlternateEnclosure>?.firstHlsStream(): AlternateEnclosureStream? = this
    ?.filter { BaseEpisode.isHlsMimeType(it.type) }
    ?.firstNotNullOfOrNull { it.toStream() }

/** The streamable non-HLS encoding the server marked `media_kind = "video"`, such as a `video/mp4` enclosure. */
fun List<EpisodeAlternateEnclosure>?.firstProgressiveVideoStream(): AlternateEnclosureStream? = this
    ?.filter { it.isProgressiveVideo }
    ?.firstNotNullOfOrNull { it.toStream() }

/** MIME type to record on an episode the server sent no progressive enclosure for. */
fun List<EpisodeAlternateEnclosure>?.firstStreamOnlyMimeType(): String? = firstHlsMimeType() ?: this?.firstOrNull { it.isProgressiveVideo }?.type

private val EpisodeAlternateEnclosure.isProgressiveVideo: Boolean
    get() = mediaKind == MediaKind.Video && !BaseEpisode.isHlsMimeType(type)

private fun EpisodeAlternateEnclosure.toStream(): AlternateEnclosureStream? {
    val source = sources.firstOrNull { it.uri.isPlayableHttpUri() } ?: return null
    // The enclosure's own type describes the encoding; a source may declare a generic type such as application/octet-stream.
    val contentType = type?.takeIf { it.isNotBlank() } ?: source.contentType?.takeIf { it.isNotBlank() }
    return AlternateEnclosureStream(url = source.uri, contentType = contentType, mediaKind = mediaKind)
}

private fun String.isPlayableHttpUri(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}
