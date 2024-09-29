package au.com.shiftyjelly.pocketcasts.repositories.di

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
@OptIn(UnstableApi::class)
object PlayerModule {
    @Provides
    fun providesSubtitleParserFactory(): SubtitleParser.Factory {
        return object : SubtitleParser.Factory {
            override fun supportsFormat(format: Format): Boolean {
                return when (format.sampleMimeType) {
                    in TranscriptFormat.VTT.possibleMimeTypes(),
                    in TranscriptFormat.SRT.possibleMimeTypes(),
                    -> true

                    else -> false
                }
            }

            override fun getCueReplacementBehavior(format: Format): Int {
                return when (val mimeType = format.sampleMimeType) {
                    in TranscriptFormat.VTT.possibleMimeTypes() -> WebvttParser.CUE_REPLACEMENT_BEHAVIOR
                    in TranscriptFormat.SRT.possibleMimeTypes() -> SubripParser.CUE_REPLACEMENT_BEHAVIOR
                    else -> throw IllegalArgumentException("Unsupported MIME type: $mimeType")
                }
            }

            override fun create(format: Format): SubtitleParser {
                return when (val mimeType = format.sampleMimeType) {
                    in TranscriptFormat.VTT.possibleMimeTypes() -> WebvttParser()
                    in TranscriptFormat.SRT.possibleMimeTypes() -> SubripParser()
                    else -> throw IllegalArgumentException("Unsupported MIME type: $mimeType")
                }
            }
        }
    }
}
