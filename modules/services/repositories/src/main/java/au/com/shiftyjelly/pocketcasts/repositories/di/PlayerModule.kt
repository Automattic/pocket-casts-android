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
                    TranscriptFormat.VTT.mimeType,
                    TranscriptFormat.SRT.mimeType,
                    -> true

                    else -> false
                }
            }

            override fun getCueReplacementBehavior(format: Format): Int {
                return when (val mimeType = format.sampleMimeType) {
                    TranscriptFormat.VTT.mimeType -> WebvttParser.CUE_REPLACEMENT_BEHAVIOR
                    TranscriptFormat.SRT.mimeType -> SubripParser.CUE_REPLACEMENT_BEHAVIOR
                    else -> throw IllegalArgumentException("Unsupported MIME type: $mimeType")
                }
            }

            override fun create(format: Format): SubtitleParser {
                return when (val mimeType = format.sampleMimeType) {
                    TranscriptFormat.VTT.mimeType -> WebvttParser()
                    TranscriptFormat.SRT.mimeType -> SubripParser()
                    else -> throw IllegalArgumentException("Unsupported MIME type: $mimeType")
                }
            }
        }
    }
}
