package au.com.shiftyjelly.pocketcasts.wear.data.service.log

import androidx.media3.common.Format
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.util.EventLogger
import com.google.android.horologist.media3.logging.ErrorReporter
import java.io.IOException

/**
 * Simple implementation of EventLogger for logging critical Media3 events.
 *
 * Most logging behaviour is inherited from EventLogger.
 */
class AnalyticsEventLogger(
    private val appEventLogger: ErrorReporter
) : EventLogger("ErrorReporter") {
    override fun onAudioSinkError(
        eventTime: AnalyticsListener.EventTime,
        audioSinkError: Exception
    ) {
        appEventLogger.logMessage(
            "onAudioSinkError $audioSinkError",
            category = ErrorReporter.Category.Playback,
            level = ErrorReporter.Level.Error
        )
    }

    override fun onAudioCodecError(
        eventTime: AnalyticsListener.EventTime,
        audioCodecError: Exception
    ) {
        appEventLogger.logMessage(
            "onAudioCodecError $audioCodecError",
            category = ErrorReporter.Category.Playback,
            level = ErrorReporter.Level.Error
        )
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        appEventLogger.logMessage(
            "onPlaybackStateChanged $state",
            category = ErrorReporter.Category.Playback
        )
        super.onPlaybackStateChanged(eventTime, state)
    }

    override fun onPlayWhenReadyChanged(
        eventTime: AnalyticsListener.EventTime,
        playWhenReady: Boolean,
        reason: Int
    ) {
        appEventLogger.logMessage(
            "onPlayWhenReadyChanged $playWhenReady $reason",
            category = ErrorReporter.Category.Playback,
            level = ErrorReporter.Level.Debug
        )
        super.onPlayWhenReadyChanged(eventTime, playWhenReady, reason)
    }

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long
    ) {
        appEventLogger.logMessage(
            "onAudioUnderrun $elapsedSinceLastFeedMs",
            category = ErrorReporter.Category.Playback,
            level = ErrorReporter.Level.Error
        )
        super.onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
    }

    override fun onIsLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {
        appEventLogger.logMessage(
            "onIsLoadingChanged $isLoading",
            category = ErrorReporter.Category.Playback
        )
        super.onIsLoadingChanged(eventTime, isLoading)
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        appEventLogger.logMessage(
            "onLoadError $error",
            category = ErrorReporter.Category.Playback,
            level = ErrorReporter.Level.Error
        )
        super.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled)
    }

    override fun onMediaMetadataChanged(
        eventTime: AnalyticsListener.EventTime,
        mediaMetadata: MediaMetadata
    ) {
        appEventLogger.logMessage(
            "onMediaMetadataChanged ${mediaMetadata.displayTitle}",
            category = ErrorReporter.Category.Playback
        )
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        appEventLogger.logMessage(
            "onPlayerError $error",
            category = ErrorReporter.Category.Playback,
            level = ErrorReporter.Level.Error
        )
        super.onPlayerError(eventTime, error)
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        appEventLogger.logMessage(
            "onLoadStarted",
            category = ErrorReporter.Category.Playback
        )
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        appEventLogger.logMessage(
            "onLoadCompleted",
            category = ErrorReporter.Category.Playback
        )
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?
    ) {
        appEventLogger.logMessage(
            "onAudioInputFormatChanged ${format.codecs.orEmpty()} ${format.bitrate} ${format.containerMimeType.orEmpty()}",
            level = ErrorReporter.Level.Debug,
            category = ErrorReporter.Category.Playback
        )
        super.onAudioInputFormatChanged(eventTime, format, decoderReuseEvaluation)
    }

    override fun onDownstreamFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        mediaLoadData: MediaLoadData
    ) {
        appEventLogger.logMessage(
            "onDownstreamFormatChanged ${mediaLoadData.dataType}",
            category = ErrorReporter.Category.Playback
        )
        super.onDownstreamFormatChanged(eventTime, mediaLoadData)
    }

    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long
    ) {
        appEventLogger.logMessage(
            "onBandwidthEstimate $bitrateEstimate",
            level = ErrorReporter.Level.Debug
        )
    }
}
