package au.com.shiftyjelly.pocketcasts.models.to

sealed interface Transcript {
    val type: TranscriptType
    val url: String
    val isGenerated: Boolean
    val episodeUuid: String
    val podcastUuid: String?

    data class Text(
        val entries: List<TranscriptEntry>,
        override val type: TranscriptType,
        override val url: String,
        override val isGenerated: Boolean,
        override val episodeUuid: String,
        override val podcastUuid: String?,
    ) : Transcript

    data class Web(
        override val url: String,
        override val isGenerated: Boolean,
        override val episodeUuid: String,
        override val podcastUuid: String?,
    ) : Transcript {
        override val type get() = TranscriptType.Html
    }
}
