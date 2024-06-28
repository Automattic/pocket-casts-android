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
        createBaseEvent(AnalyticsEvent.CLIP_SCREEN_PLAY_TAPPED)
    }

    fun pauseTapped() {
        createBaseEvent(AnalyticsEvent.CLIP_SCREEN_PAUSE_TAPPED)
    }

    fun linkShared(clip: Clip) {
        val isStartModified = initialClipRange.start != clip.range.start
        val isEndMofidied = initialClipRange.end != clip.range.end
        createBaseEvent(
            AnalyticsEvent.CLIP_SCREEN_SHOWN,
            mapOf(
                "start" to clip.range.start,
                "end" to clip.range.end,
                "start_modified" to isStartModified,
                "end_modified" to isEndMofidied,
            ),
        )
    }

    private fun trackEvent(event: Event) {
        analyticsTracker.track(event.type, event.properties)
    }

    private fun createBaseEvent(type: AnalyticsEvent, properties: Map<String, Any> = emptyMap()) = Event(
        type,
        properties + mapOf(
            "episode_id" to episodeId,
            "podcast_id" to podcastId,
            "clip_id" to clipId,
            "source" to source.analyticsValue,
        ),
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
