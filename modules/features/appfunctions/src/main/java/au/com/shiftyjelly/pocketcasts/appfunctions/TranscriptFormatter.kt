package au.com.shiftyjelly.pocketcasts.appfunctions

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import java.util.Locale

internal fun Transcript.Text.toTranscriptResult(fromSeconds: Int, toSeconds: Int, maxChars: Int): TranscriptResult {
    val text = StringBuilder()
    var speaker: String? = null
    var isTruncated = false
    for (entry in entries) {
        when (entry) {
            is TranscriptEntry.Speaker -> speaker = entry.name

            is TranscriptEntry.Text -> {
                val startSeconds = (entry.startTimeMs / 1000).toInt().takeIf { entry.startTimeMs >= 0 }
                if (startSeconds != null && startSeconds < fromSeconds) continue
                if (startSeconds != null && toSeconds > 0 && startSeconds > toSeconds) break
                val line = buildString {
                    startSeconds?.let { append("[${it.toTimestamp()}] ") }
                    speaker?.let { append("$it: ") }
                    append(entry.value.trim())
                }
                if (text.length + line.length + 1 > maxChars) {
                    isTruncated = true
                    break
                }
                text.appendLine(line)
                speaker = null
            }
        }
    }
    return TranscriptResult(text = text.toString().trimEnd(), isGenerated = isGenerated, isTruncated = isTruncated)
}

private fun Int.toTimestamp() = String.format(Locale.ROOT, "%d:%02d:%02d", this / 3600, this / 60 % 60, this % 60)
