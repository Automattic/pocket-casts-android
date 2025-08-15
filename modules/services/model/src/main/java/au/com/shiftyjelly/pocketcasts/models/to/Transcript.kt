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
    ) : Transcript {
        fun buildString(): String {
            return entries.joinToString(separator = "\n") { entry ->
                when (entry) {
                    is TranscriptEntry.Text -> entry.value
                    is TranscriptEntry.Speaker -> entry.name
                }
            }
        }
    }

    data class Web(
        override val url: String,
        override val isGenerated: Boolean,
        override val episodeUuid: String,
        override val podcastUuid: String?,
    ) : Transcript {
        override val type get() = TranscriptType.Html
    }

    companion object {
        val TextPreview
            get() = Transcript.Text(
                entries = TranscriptEntry.PreviewList,
                type = TranscriptType.Vtt,
                url = "https://pocketacsts.com/transcript.json",
                isGenerated = false,
                episodeUuid = "episode-uuid",
                podcastUuid = "podcast-uuid",
            )

        val WebPreview
            get() = Transcript.Web(
                url = "https://pocketacsts.com/transcript.json",
                isGenerated = false,
                episodeUuid = "episode-uuid",
                podcastUuid = "podcast-uuid",
            )
    }
}
