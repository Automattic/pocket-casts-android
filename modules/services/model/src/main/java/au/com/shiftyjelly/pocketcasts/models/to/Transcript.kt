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
        fun getExcerpt(): String {
            val concatenatedEntries = buildString {
                entries.asSequence()
                    .filterIsInstance<TranscriptEntry.Text>()
                    .takeWhile { length <= 140 }
                    .forEachIndexed { index, entry ->
                        if (index != 0) {
                            append(' ')
                        }
                        append(entry.value)
                    }
            }
            return concatenatedEntries
                .take(140)
                .trimEnd()
                .let { string ->
                    val shouldAppendEllipsis = concatenatedEntries.length > 140
                    if (shouldAppendEllipsis) {
                        string + "…"
                    } else {
                        string
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
