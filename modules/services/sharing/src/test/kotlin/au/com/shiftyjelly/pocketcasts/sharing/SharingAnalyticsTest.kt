package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SharingAnalyticsTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val tracker = TestTracker()

    private val analytics = SharingAnalytics(AnalyticsTracker.test(tracker, isEnabled = true))

    private val podcast = Podcast()
    private val episode = PodcastEpisode(uuid = "uuid", publishedDate = Date())
    private val position = 10.seconds
    private val clipRange = Clip.Range(15.seconds, 33.seconds)

    @Test
    fun `log podcast sharing`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "podcast",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log episode sharing`() {
        val request = SharingRequest.episode(podcast, episode)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "episode",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log episode position sharing`() {
        val request = SharingRequest.episodePosition(podcast, episode, position)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "current_time",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log bookmark sharing`() {
        val request = SharingRequest.bookmark(podcast, episode, position)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "bookmark_time",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log episode file sharing`() {
        val request = SharingRequest.episodeFile(podcast, episode)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "episode_file",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log clip link sharing`() {
        val request = SharingRequest.clipLink(podcast, episode, clipRange)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "clip_link",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log clip audio sharing`() {
        val request = SharingRequest.audioClip(podcast, episode, clipRange)
            .setPlatform(SocialPlatform.PocketCasts)
            .setCardType(CardType.Vertical)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "clip_audio",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log clip video sharing`() {
        val request = SharingRequest.videoClip(podcast, episode, clipRange, CardType.Vertical, tempFolder.newFile())
            .setPlatform(SocialPlatform.PocketCasts)
            .setSourceView(SourceView.PLAYER)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertType(AnalyticsEvent.PODCAST_SHARED)
        event.assertProperties(
            mapOf(
                "type" to "clip_video",
                "action" to "url",
                "card_type" to "vertical",
                "source" to "player",
            ),
        )
    }

    @Test
    fun `log source property`() {
        SourceView.entries.forEach { source ->
            val request = SharingRequest.podcast(podcast)
                .setSourceView(source)
                .build()

            analytics.onShare(request)
            val event = tracker.events.last()

            event.assertProperty("source", source.analyticsValue)
        }
    }

    @Test
    fun `log podcast type property`() {
        val request = SharingRequest.podcast(podcast).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "podcast")
    }

    @Test
    fun `log epiosde type property`() {
        val request = SharingRequest.episode(podcast, episode).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "episode")
    }

    @Test
    fun `log epiosde position type property`() {
        val request = SharingRequest.episodePosition(podcast, episode, position).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "current_time")
    }

    @Test
    fun `log bookmark type property`() {
        val request = SharingRequest.bookmark(podcast, episode, position).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "bookmark_time")
    }

    @Test
    fun `log episode file type property`() {
        val request = SharingRequest.episodeFile(podcast, episode).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "episode_file")
    }

    @Test
    fun `log clip link type property`() {
        val request = SharingRequest.clipLink(podcast, episode, clipRange).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "clip_link")
    }

    @Test
    fun `log clip audio type property`() {
        val request = SharingRequest.audioClip(podcast, episode, clipRange).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "clip_audio")
    }

    @Test
    fun `log clip video type property`() {
        val request = SharingRequest.videoClip(podcast, episode, clipRange, CardType.Vertical, tempFolder.newFile()).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("type", "clip_video")
    }

    @Test
    fun `log instagram story action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.Instagram)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "ig_story")
    }

    @Test
    fun `log whats app action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.WhatsApp)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "whats_app")
    }

    @Test
    fun `log telegram action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.Telegram)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "telegram")
    }

    @Test
    fun `log twitter action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.X)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "twitter")
    }

    @Test
    fun `log tumblr action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.Tumblr)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "tumblr")
    }

    @Test
    fun `log url action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.PocketCasts)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "url")
    }

    @Test
    fun `log system sheet action property`() {
        val request = SharingRequest.podcast(podcast)
            .setPlatform(SocialPlatform.More)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("action", "system_sheet")
    }

    @Test
    fun `log vertical card type property`() {
        val request = SharingRequest.podcast(podcast)
            .setCardType(CardType.Vertical)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("card_type", "vertical")
    }

    @Test
    fun `log horizontal card type property`() {
        val request = SharingRequest.podcast(podcast)
            .setCardType(CardType.Horizontal)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("card_type", "horizontal")
    }

    @Test
    fun `log square card type property`() {
        val request = SharingRequest.podcast(podcast)
            .setCardType(CardType.Square)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("card_type", "square")
    }

    @Test
    fun `log square audio type property`() {
        val request = SharingRequest.podcast(podcast)
            .setCardType(CardType.Audio)
            .build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("card_type", "audio")
    }

    @Test
    fun `log no card type property`() {
        val request = SharingRequest.podcast(podcast).build()

        analytics.onShare(request)
        val event = tracker.events.single()

        event.assertProperty("card_type", null)
    }

    class TestTracker : Tracker {
        private val _events = mutableListOf<TrackEvent>()

        val events get() = _events.toList()

        override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
            _events += TrackEvent(event, properties)
        }

        override fun refreshMetadata() = Unit

        override fun flush() = Unit

        override fun clearAllData() = Unit
    }

    data class TrackEvent(
        val type: AnalyticsEvent,
        val properties: Map<String, Any>,
    ) {
        fun assertType(type: AnalyticsEvent) {
            assertEquals(type, this.type)
        }

        fun assertProperty(key: String, value: Any?) {
            assertEquals(value, properties[key])
        }

        fun assertProperties(properties: Map<String, Any>) {
            assertEquals(properties, this.properties)
        }
    }
}
