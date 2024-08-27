package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.media3.common.Format
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import au.com.shiftyjelly.pocketcasts.models.converter.TranscriptJsonConverter
import au.com.shiftyjelly.pocketcasts.models.to.CuesAdditionalInfo
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptCuesInfo
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.ParsingException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.common.collect.ImmutableList
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import okhttp3.ResponseBody
import okhttp3.internal.toImmutableList

@UnstableApi
class TranscriptCuesInfoBuilder @Inject constructor(
    private val subtitleParserFactory: SubtitleParser.Factory,
    private val transcriptJsonConverter: TranscriptJsonConverter,
) {
    fun build(
        transcript: Transcript,
        transcriptResponse: ResponseBody?,
    ): List<TranscriptCuesInfo> =
        when (transcript.type) {
            TranscriptFormat.HTML.mimeType -> {
                val content = transcriptResponse?.string() ?: ""
                if (content.trim().isEmpty()) {
                    throw EmptyDataException("Transcript content is empty ${transcript.url}")
                } else {
                    // Html content is added as single large cue
                    ImmutableList.of(
                        CuesWithTiming(
                            ImmutableList.of(Cue.Builder().setText(content).build()),
                            0,
                            0,
                        ).toTranscriptCuesInfo(),
                    )
                }
            }

            TranscriptFormat.JSON_PODCAST_INDEX.mimeType -> {
                val jsonString = transcriptResponse?.string() ?: ""
                if (jsonString.trim().isEmpty()) {
                    throw EmptyDataException("Transcript content is empty ${transcript.url}")
                } else {
                    // Parse json following PodcastIndex.org transcript json spec: https://github.com/Podcastindex-org/podcast-namespace/blob/main/transcripts/transcripts.md#json
                    val transcriptCues = transcriptJsonConverter.fromString(jsonString)
                    transcriptCues.map { cue ->
                        val startTimeUs = cue.startTime?.toMicroSeconds ?: 0
                        val endTimeUs = cue.endTime?.toMicroSeconds ?: 0
                        CuesWithTiming(
                            ImmutableList.of(Cue.Builder().setText(cue.body ?: "").build()),
                            startTimeUs,
                            endTimeUs - startTimeUs,
                        ).toTranscriptCuesInfo(
                            cuesAdditionalInfo = CuesAdditionalInfo(speaker = cue.speaker),
                        )
                    }.toImmutableList()
                }
            }

            else -> {
                val format = Format.Builder()
                    .setSampleMimeType(transcript.type)
                    .build()
                if (subtitleParserFactory.supportsFormat(format).not()) {
                    throw UnsupportedOperationException("Unsupported MIME type: ${transcript.type}")
                } else {
                    val result = ImmutableList.builder<CuesWithTiming>()
                    transcriptResponse?.bytes()?.let { data ->
                        try {
                            val parser = subtitleParserFactory.create(format)
                            parser.parse(
                                data,
                                SubtitleParser.OutputOptions.allCues(),
                            ) { element: CuesWithTiming? ->
                                element?.let { result.add(it) }
                            }
                        } catch (e: Exception) {
                            val message = "Failed to parse transcript: ${transcript.url}"
                            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, e, message)
                            throw ParsingException(message)
                        }
                    }
                    val cuesInfo = result.build().map { it.toTranscriptCuesInfo() }
                    cuesInfo.ifEmpty { throw throw EmptyDataException("Transcript content is empty ${transcript.url}") }
                }
            }
        }

    private val Double.toMicroSeconds: Long
        get() = toDuration(DurationUnit.SECONDS).inWholeMicroseconds

    private fun CuesWithTiming.toTranscriptCuesInfo(
        cuesAdditionalInfo: CuesAdditionalInfo? = null,
    ) = TranscriptCuesInfo(this, cuesAdditionalInfo)
}
