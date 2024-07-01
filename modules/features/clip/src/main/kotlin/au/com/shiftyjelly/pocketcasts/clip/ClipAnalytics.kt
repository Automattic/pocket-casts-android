package au.com.shiftyjelly.pocketcasts.clip

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ClipAnalytics @AssistedInject constructor(
    @Assisted("episodeId") private val episodeId: String,
    @Assisted("podcastId") private val podcastId: String,
    @Assisted("clipId") private val clipId: String,
    @Assisted private val source: SourceView,
    @Assisted private val initialClipRange: Clip.Range,
    val analyticsTracker: AnalyticsTracker,
) {
    fun screenShown() {
        trackEvent(createBaseEvent(AnalyticsEvent.CLIP_SCREEN_SHOWN))
    }

    fun playTapped() {
        trackEvent(createBaseEvent(AnalyticsEvent.CLIP_SCREEN_PLAY_TAPPED))
    }

    fun pauseTapped() {
        trackEvent(createBaseEvent(AnalyticsEvent.CLIP_SCREEN_PAUSE_TAPPED))
    }

    fun linkShared(clip: Clip) {
        val isStartModified = initialClipRange.start.inWholeSeconds != clip.range.start.inWholeSeconds
        val isEndMofidied = initialClipRange.end.inWholeSeconds != clip.range.end.inWholeSeconds
        val event = createBaseEvent(
            AnalyticsEvent.CLIP_SCREEN_LINK_SHARED,
            mapOf(
                "start" to clip.range.start.inWholeSeconds.toInt(),
                "end" to clip.range.end.inWholeSeconds.toInt(),
                "start_modified" to isStartModified,
                "end_modified" to isEndMofidied,
            ),
        )
        trackEvent(event)
    }

    private fun trackEvent(event: Event) {
        analyticsTracker.track(event.type, event.properties)
    }

    private fun createBaseEvent(type: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) = Event(
        type,
        mapOf(
            "episode_uuid" to episodeId,
            "podcast_uuid" to podcastId,
            "clip_uuid" to clipId,
            "source" to source.analyticsValue,
        ) + properties,
    )

    private class Event(
        val type: AnalyticsEvent,
        val properties: Map<String, Any>,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("episodeId") episodeId: String,
            @Assisted("podcastId") podcastId: String,
            @Assisted("clipId") clipId: String,
            sourceView: SourceView,
            initialClipRange: Clip.Range,
        ): ClipAnalytics
    }
}
