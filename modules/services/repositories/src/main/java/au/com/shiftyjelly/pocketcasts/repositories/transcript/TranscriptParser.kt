@file:OptIn(UnstableApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.transcript

import android.text.SpannedString
import androidx.annotation.OptIn
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.media3.common.text.Cue
import androidx.media3.common.text.VoiceSpan
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import au.com.shiftyjelly.pocketcasts.models.converter.TranscriptCue
import au.com.shiftyjelly.pocketcasts.models.converter.TranscriptSegments
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import com.squareup.moshi.Moshi
import kotlin.math.roundToLong
import okio.Buffer
import okio.BufferedSource
import okio.use
import androidx.media3.extractor.text.SubtitleParser as Media3SubtitleParser

interface TranscriptParser {
    val type: TranscriptType

    fun parse(source: BufferedSource): Result<List<TranscriptEntry>>
}

internal abstract class SubtitleParser(
    private val delegate: Media3SubtitleParser,
) : TranscriptParser {
    override fun parse(source: BufferedSource) = runCatching {
        val data = source.use { it.readByteArray() }
        parseAll(data).flatMap { cuesWithTiming ->
            val startTimeMs = cuesWithTiming.startTimeUs / 1000
            val endTimeMs = cuesWithTiming.endTimeUs / 1000
            cuesWithTiming.cues.flatMap { cue -> toEntries(cue, startTimeMs, endTimeMs) }
        }
    }

    private fun parseAll(data: ByteArray): List<CuesWithTiming> {
        return buildList {
            delegate.parse(data, 0, data.size, Media3SubtitleParser.OutputOptions.allCues()) {
                add(it)
            }
        }
    }

    protected abstract fun toEntries(cue: Cue, startTimeMs: Long, endTimeMs: Long): List<TranscriptEntry>
}

internal class WebVttParser : SubtitleParser(WebvttParser()) {
    override val type get() = TranscriptType.Vtt

    override fun toEntries(cue: Cue, startTimeMs: Long, endTimeMs: Long): List<TranscriptEntry> {
        val cueText = cue.text
        if (cueText.isNullOrEmpty()) {
            return emptyList()
        }

        return buildList {
            if (cueText is SpannedString) {
                val speakers = cueText.getSpans<VoiceSpan>(0, cueText.length)
                    .sortedBy { cueText.getSpanStart(it) }
                    .joinToString(separator = ", ") { voiceSpan -> voiceSpan.name }
                if (speakers.isNotEmpty()) {
                    add(TranscriptEntry.Speaker(speakers))
                }
            }
            add(TranscriptEntry.Text(cueText.toString(), startTimeMs = startTimeMs, endTimeMs = endTimeMs))
        }
    }
}

internal class SrtParser : SubtitleParser(SubripParser()) {
    override val type get() = TranscriptType.Srt

    override fun toEntries(cue: Cue, startTimeMs: Long, endTimeMs: Long): List<TranscriptEntry> {
        val cueText = cue.text?.toString()
        if (cueText.isNullOrEmpty()) {
            return emptyList()
        }

        return buildList {
            val speakerGroups = SpeakerRegex.matchEntire(cueText)?.groupValues
            if (speakerGroups != null) {
                add(TranscriptEntry.Speaker(speakerGroups[1]))
                add(TranscriptEntry.Text(speakerGroups[2], startTimeMs = startTimeMs, endTimeMs = endTimeMs))
            } else {
                add(TranscriptEntry.Text(cueText, startTimeMs = startTimeMs, endTimeMs = endTimeMs))
            }
        }
    }

    private companion object {
        val SpeakerRegex = """^\s*([a-zA-Z0-9 ]+):\s(.*)""".toRegex()
    }
}

internal class HtmlParser : TranscriptParser {
    override val type get() = TranscriptType.Html

    override fun parse(source: BufferedSource) = runCatching {
        val text = source.use { it.readUtf8() }
        // Having a script tag most likely means that we should redirect to a web page
        if ("</script>" in text) {
            throw ScriptDetectedException()
        }

        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .lines()
            .mapNotNull { line -> line.trim().takeIf(String::isNotEmpty) }
            .flatMap { line ->
                buildList {
                    val speakerGroups = SpeakerRegex.matchEntire(line)?.groupValues
                    if (speakerGroups != null) {
                        add(TranscriptEntry.Speaker(speakerGroups[1]))
                        add(TranscriptEntry.Text(speakerGroups[2]))
                    } else {
                        add(TranscriptEntry.Text(line))
                    }
                }
            }
    }

    class ScriptDetectedException : RuntimeException()

    private companion object {
        val SpeakerRegex = """^([a-zA-Z0-9 ]+):\s(.*)""".toRegex()
    }
}

internal class JsonParser(
    moshi: Moshi,
) : TranscriptParser {
    override val type get() = TranscriptType.Json

    private val adapter = moshi.adapter(TranscriptSegments::class.java)
    private val vttParser by lazy { WebVttParser() }

    override fun parse(source: BufferedSource) = runCatching {
        val data = requireNotNull(adapter.fromJson(source))
        val segments = data.segments
        val firstSegment = segments.firstOrNull()
        val vtt = data.vtt

        when {
            // Podcast Index style: segments carry a speaker and a body.
            firstSegment?.speaker != null && firstSegment.body != null -> {
                segments.flatMap(::toEntries)
            }
            // Flightcast style: prefer the embedded WEBVTT document when present.
            vtt != null && vtt.contains("WEBVTT") -> {
                vttParser.parse(Buffer().writeUtf8(vtt)).getOrThrow()
            }
            // Fallback: segments carrying plain text (Flightcast) or body-only entries.
            else -> segments.flatMap(::toEntries)
        }
    }

    private fun toEntries(cue: TranscriptCue) = buildList<TranscriptEntry> {
        cue.speaker?.let { speaker ->
            add(TranscriptEntry.Speaker(speaker))
        }
        val content = cue.content ?: return@buildList
        val startTimeMs = cue.startSeconds?.let { (it * 1000).roundToLong() } ?: -1L
        val endTimeMs = cue.endSeconds?.let { (it * 1000).roundToLong() } ?: -1L
        add(TranscriptEntry.Text(content, startTimeMs = startTimeMs, endTimeMs = endTimeMs))
    }
}
