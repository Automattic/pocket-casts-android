package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import com.automattic.eventhorizon.EndOfYearStoryShareEvent
import com.automattic.eventhorizon.EndOfYearStoryType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PodcastSharedEvent
import com.automattic.eventhorizon.ReferralPassSharedEvent
import com.automattic.eventhorizon.ShareActionCardType
import com.automattic.eventhorizon.ShareActionPlatform
import com.automattic.eventhorizon.ShareActionType
import java.time.Year
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.automattic.eventhorizon.SourceView as EventHorizonSourceView

class SharingAnalyticsTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val eventSink = TestEventSink()

    private val analytics = SharingAnalytics(EventHorizon(eventSink))

    private val podcast = Podcast()
    private val episode = PodcastEpisode(uuid = "uuid", publishedDate = Date())
    private val position = 10.seconds
    private val clipRange = Clip.Range(15.seconds, 33.seconds)

    @Test
    fun `log podcast sharing`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.Podcast,
                action = ShareActionPlatform.Url,
                cardType = ShareActionCardType.Vertical,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log episode sharing`() {
        val request = SharingRequest
            .episode(
                podcast = podcast,
                episode = episode,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.Episode,
                action = ShareActionPlatform.Url,
                cardType = ShareActionCardType.Vertical,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log episode position sharing`() {
        val request = SharingRequest
            .episodePosition(
                podcast = podcast,
                episode = episode,
                position = position,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.CurrentTime,
                action = ShareActionPlatform.Url,
                cardType = ShareActionCardType.Vertical,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log bookmark sharing`() {
        val request = SharingRequest
            .bookmark(
                podcast = podcast,
                episode = episode,
                position = position,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.BookmarkTime,
                action = ShareActionPlatform.Url,
                cardType = ShareActionCardType.Vertical,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log episode file sharing`() {
        val request = SharingRequest
            .episodeFile(
                podcast = podcast,
                episode = episode,
                source = SourceView.PLAYER,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.EpisodeFile,
                action = ShareActionPlatform.SystemSheet,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log clip link sharing`() {
        val request = SharingRequest
            .clipLink(
                podcast = podcast,
                episode = episode,
                range = clipRange,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.ClipLink,
                action = ShareActionPlatform.Url,
                cardType = ShareActionCardType.Vertical,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log clip audio sharing`() {
        val request = SharingRequest
            .audioClip(
                podcast = podcast,
                episode = episode,
                range = clipRange,
                source = SourceView.PLAYER,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.ClipAudio,
                action = ShareActionPlatform.SystemSheet,
                cardType = ShareActionCardType.Audio,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log clip video sharing`() {
        val request = SharingRequest
            .videoClip(
                podcast = podcast,
                episode = episode,
                range = clipRange,
                backgroundImage = tempFolder.newFile(),
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            PodcastSharedEvent(
                source = EventHorizonSourceView.Player,
                type = ShareActionType.ClipVideo,
                action = ShareActionPlatform.Url,
                cardType = ShareActionCardType.Vertical,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log source property`() {
        SourceView.entries.forEach { source ->
            val request = SharingRequest
                .podcast(
                    podcast = podcast,
                    source = source,
                    platform = SocialPlatform.PocketCasts,
                    cardType = CardType.Vertical,
                )
                .build()

            analytics.onShare(request)

            val event = eventSink.pollEvent() as PodcastSharedEvent
            assertEquals(source.eventHorizonValue, event.source)
        }
    }

    @Test
    fun `log instagram story action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.Instagram,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.IgStory, event.action)
    }

    @Test
    fun `log whats app action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.WhatsApp,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.WhatsApp, event.action)
    }

    @Test
    fun `log telegram action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.Telegram,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.Telegram, event.action)
    }

    @Test
    fun `log twitter action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.X,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.Twitter, event.action)
    }

    @Test
    fun `log tumblr action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.Tumblr,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.Tumblr, event.action)
    }

    @Test
    fun `log url action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.Url, event.action)
    }

    @Test
    fun `log system sheet action property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.More,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionPlatform.SystemSheet, event.action)
    }

    @Test
    fun `log vertical card type property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Vertical,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionCardType.Vertical, event.cardType)
    }

    @Test
    fun `log horizontal card type property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Horizontal,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionCardType.Horizontal, event.cardType)
    }

    @Test
    fun `log square card type property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Square,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionCardType.Square, event.cardType)
    }

    @Test
    fun `log square audio type property`() {
        val request = SharingRequest
            .podcast(
                podcast = podcast,
                source = SourceView.PLAYER,
                platform = SocialPlatform.PocketCasts,
                cardType = CardType.Audio,
            )
            .build()

        analytics.onShare(request)

        val event = eventSink.pollEvent() as PodcastSharedEvent
        assertEquals(ShareActionCardType.Audio, event.cardType)
    }

    @Test
    fun `log referral link sharing`() {
        val referralCode = "TEST_CODE"
        val request = SharingRequest
            .referralLink(
                referralCode = referralCode,
                offerName = "offer-name",
                offerDuration = "offer-duration",
                source = SourceView.PLAYER,
            )
            .build()

        analytics.onShare(request)

        assertEquals(
            ReferralPassSharedEvent(
                source = EventHorizonSourceView.Player,
                code = referralCode,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log number of shows story sharing`() {
        val story = Story.NumberOfShows(
            showCount = 100,
            episodeCount = 200,
            randomShowIds = emptyList(),
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.NumberOfShows,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log top 1 show story sharing`() {
        val story = Story.TopShow(
            show = TopPodcast(
                uuid = "podcast-id",
                title = "podcast-title",
                author = "pocast-author",
                playbackTimeSeconds = 0.0,
                playedEpisodeCount = 0,
            ),
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.Top1Show,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log top 5 shows story sharing`() {
        val story = Story.TopShows(
            shows = emptyList(),
            podcastListUrl = null,
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.Top5Shows,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log ratings story sharing`() {
        val story = Story.Ratings(
            stats = RatingStats(
                ones = 10,
                twos = 20,
                threes = 30,
                fours = 40,
                fives = 50,
            ),
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.Ratings,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log total time story sharing`() {
        val story = Story.TotalTime(
            duration = 12345.seconds,
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.TotalTime,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log longest episode story sharing`() {
        val story = Story.LongestEpisode(
            episode = LongestEpisode(
                episodeId = "episode-id",
                episodeTitle = "",
                podcastId = "",
                podcastTitle = "",
                durationSeconds = 0.0,
                coverUrl = null,
            ),
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.LongestEpisode,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log year vs year story sharing`() {
        val story = Story.YearVsYear(
            lastYearDuration = Duration.ZERO,
            thisYearDuration = Duration.ZERO,
            subscriptionTier = null,
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.YearVsYear,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `log completion rate story sharing`() {
        val story = Story.CompletionRate(
            listenedCount = 0,
            completedCount = 0,
            subscriptionTier = null,
        )
        val request = SharingRequest
            .endOfYearStory(story, Year.of(1000), tempFolder.newFile())
            .build()

        analytics.onShare(request)

        assertEquals(
            EndOfYearStoryShareEvent(
                story = EndOfYearStoryType.CompletionRate,
                currentYear = 1000,
            ),
            eventSink.pollEvent(),
        )
    }
}
