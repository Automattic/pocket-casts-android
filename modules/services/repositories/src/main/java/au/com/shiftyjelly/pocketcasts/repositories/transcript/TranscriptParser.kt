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
import com.squareup.moshi.Moshi
import okio.BufferedSource
import okio.use
import androidx.media3.extractor.text.SubtitleParser as Media3SubtitleParser

internal interface TranscriptParser {
    fun parse(source: BufferedSource): Result<List<TranscriptEntry>>
}

internal abstract class SubtitleParser(
    private val delegate: Media3SubtitleParser,
) : TranscriptParser {
    override fun parse(source: BufferedSource) = runCatching {
        val data = source.use { it.readByteArray() }
        parseAll(data).flatMap { cuesWithTiming ->
            cuesWithTiming.cues.flatMap { cue -> toEntries(cue) }
        }
    }

    private fun parseAll(data: ByteArray): List<CuesWithTiming> {
        return buildList {
            delegate.parse(data, 0, data.size, Media3SubtitleParser.OutputOptions.allCues()) {
                add(it)
            }
        }
    }

    protected abstract fun toEntries(cue: Cue): List<TranscriptEntry>
}

internal class WebVttParser : SubtitleParser(WebvttParser()) {
    override fun toEntries(cue: Cue): List<TranscriptEntry> {
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
            add(TranscriptEntry.Text(cueText.toString()))
        }
    }
}

internal class SrtParser : SubtitleParser(SubripParser()) {
    override fun toEntries(cue: Cue): List<TranscriptEntry> {
        val cueText = cue.text?.toString()
        if (cueText.isNullOrEmpty()) {
            return emptyList()
        }

        return listOf(TranscriptEntry.Text(cueText.toString()))
    }
}

internal class HtmlParser : TranscriptParser {
    override fun parse(source: BufferedSource) = runCatching {
        val text = source.use { it.readUtf8() }
        // Having a script tag most likely means that we should redirect to a web page
        if ("</script>" in text) {
            throw ScriptDetectedException()
        }

        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .lines()
            .mapNotNull { line ->
                val trimmedLine = line.trim().takeIf(String::isNotEmpty)
                trimmedLine?.let(TranscriptEntry::Text)
            }
    }

    class ScriptDetectedException : RuntimeException()
}

internal class JsonParser(
    moshi: Moshi,
) : TranscriptParser {
    private val adapter = moshi.adapter(TranscriptSegments::class.java)

    override fun parse(source: BufferedSource) = runCatching {
        requireNotNull(adapter.fromJson(source))
            .segments
            .flatMap(::toEntries)
    }

    private fun toEntries(cue: TranscriptCue) = buildList<TranscriptEntry> {
        cue.speaker?.let { speaker ->
            add(TranscriptEntry.Speaker(speaker))
        }
        add(TranscriptEntry.Text(cue.body))
    }
}
