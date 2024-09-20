package au.com.shiftyjelly.pocketcasts.reimagine.clip

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ClipAnalytics @AssistedInject constructor(
    @Assisted("episodeId") private val episodeId: String,
    @Assisted("podcastId") private val podcastId: String,
    @Assisted("clipId") private val clipId: String,
    @Assisted private val source: SourceView,
    @Assisted private val initialClipRange: Clip.Range,
    private val analyticsTracker: AnalyticsTracker,
) {
    fun screenShown() {
        val event = createBaseEvent(
            AnalyticsEvent.SHARE_SCREEN_SHOWN,
            mapOf("type" to "clip"),
        )
        trackEvent(event)
    }

    fun playTapped() {
        trackEvent(createBaseEvent(AnalyticsEvent.SHARE_SCREEN_PLAY_TAPPED))
    }

    fun pauseTapped() {
        trackEvent(createBaseEvent(AnalyticsEvent.SHARE_SCREEN_PAUSE_TAPPED))
    }

    fun clipShared(
        clipRange: Clip.Range,
        shareType: ClipShareType,
        cardType: CardType,
    ) {
        val isStartModified = initialClipRange.startInSeconds != clipRange.startInSeconds
        val isEndMofidied = initialClipRange.endInSeconds != clipRange.endInSeconds
        val event = createBaseEvent(
            AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
            mapOf(
                "start" to clipRange.startInSeconds,
                "end" to clipRange.endInSeconds,
                "start_modified" to isStartModified,
                "end_modified" to isEndMofidied,
                "type" to shareType.analyticsValue,
                "card_type" to cardType.analyticsValue,
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

    private val ClipShareType.analyticsValue get() = when (this) {
        ClipShareType.Audio -> "audio"
        ClipShareType.Video -> "video"
        ClipShareType.Link -> "link"
    }

    private val CardType.analyticsValue get() = when (this) {
        CardType.Audio -> "audio"
        CardType.Horizontal -> "horizontal"
        CardType.Square -> "square"
        CardType.Vertical -> "vertical"
    }

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
