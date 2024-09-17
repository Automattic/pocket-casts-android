package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker

internal class SharingAnalytics(
    private val tracker: AnalyticsTracker,
) : SharingClient.Listener {
    override fun onShare(request: SharingRequest) {
        tracker.track(
            AnalyticsEvent.PODCAST_SHARED,
            buildMap {
                put("source", request.source.analyticsValue)
                put("type", request.data.analyticsValue)
                put("action", request.platform.analyticsValue)
                request.cardType?.analyticsValue?.let { value ->
                    put("card_type", value)
                }
            },
        )
    }

    private val SharingRequest.Data.analyticsValue get() = when (this) {
        is SharingRequest.Data.Podcast -> "podcast"
        is SharingRequest.Data.Episode -> "episode"
        is SharingRequest.Data.EpisodePosition -> when (type) {
            TimestampType.Episode -> "current_time"
            TimestampType.Bookmark -> "bookmark_time"
        }
        is SharingRequest.Data.EpisodeFile -> "episode_file"
        is SharingRequest.Data.ClipLink -> "clip_link"
        is SharingRequest.Data.ClipAudio -> "clip_audio"
        is SharingRequest.Data.ClipVideo -> "clip_video"
        is SharingRequest.Data.WebLink -> "web_link"
    }

    private val SocialPlatform.analyticsValue get() = when (this) {
        SocialPlatform.Instagram -> "ig_story"
        SocialPlatform.WhatsApp -> "whats_app"
        SocialPlatform.Telegram -> "telegram"
        SocialPlatform.X -> "twitter"
        SocialPlatform.Tumblr -> "tumblr"
        SocialPlatform.PocketCasts -> "url"
        SocialPlatform.More -> "system_sheet"
    }

    private val CardType.analyticsValue get() = when (this) {
        CardType.Vertical -> "vertical"
        CardType.Horizontal -> "horizontal"
        CardType.Square -> "square"
        CardType.Audio -> "audio"
    }
}
