package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.os.Looper
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheService
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManagerImpl
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@HiltAndroidTest
class PodcastRefresherTest {
    @get:Rule
    val server = MockWebServer()

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var podcastCacheServiceManager: PodcastCacheServiceManager

    @Inject
    lateinit var episodeManager: EpisodeManager

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var crashLogging: CrashLogging

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Before
    fun setUp() {
        // Adding a Looper for the test thread to stop the exception: Can't create handler inside thread that has not called Looper.prepare()
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        hiltRule.inject()

        val moshi = ServersModule().provideMoshi()
        val podcastCacheService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create<PodcastCacheService>()
        podcastCacheServiceManager = PodcastCacheServiceManagerImpl(podcastCacheService)
    }

    @Test
    fun refreshPodcast() {
        val podcastUuid = UUID.randomUUID().toString()
        val updatedEpisodeUuid = UUID.randomUUID().toString()
        val newEpisodeUuid = UUID.randomUUID().toString()

        val podcastCacheJson = """
            {
                "episode_frequency": "Daily",
                "estimated_next_episode_at": "2024-11-08T21:58:54Z",
                "has_seasons": false,
                "season_count": 0,
                "episode_count": 12,
                "has_more_episodes": false,
                "podcast": {
                    "url": "https://dailytechnewsshow.com",
                    "title": "Daily Tech News Show",
                    "author": "Tom Merritt",
                    "description": "Stay up to date with independent, authoritative and trustworthy tech news.",
                    "category": "Technology\n  Tech News",
                    "audio": true,
                    "show_type": null,
                    "uuid": "$podcastUuid",
                    "episodes": [
                        {
                            "uuid": "$newEpisodeUuid",
                            "title": "Will Someone Think of Tomorrow’s Children? – DTNS 4892",
                            "url": "https://open.acast.com/episode_one.mp3",
                            "file_type": "audio/mp3",
                            "file_size": 55997909,
                            "duration": 3493,
                            "published": "2024-11-07T21:58:13Z",
                            "type": "full"
                        },
                        {
                            "uuid": "$updatedEpisodeUuid",
                            "title": "Netflix Experimenting – DTNS 4891",
                            "url": "https://open.acast.com/episode_two.mp3",
                            "file_type": "audio/mp3",
                            "file_size": 46878457,
                            "duration": 2923,
                            "published": "2024-11-06T22:00:00Z",
                            "type": "full"
                        }
                    ]
                }
            }
        """.trimMargin()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody(podcastCacheJson)

        server.enqueue(mockResponse)

        val existingPodcast = Podcast(
            uuid = podcastUuid,
            title = "Daily Tech News",
            author = "Tom",
            podcastCategory = "Technology",
            podcastDescription = "Tech news",
        )

        val podcastRefresher = PodcastRefresher(
            episodeManager = episodeManager,
            appDatabase = appDatabase,
            cacheServiceManager = podcastCacheServiceManager,
            crashLogging = crashLogging,
        )

        runTest {
            val podcastDao = appDatabase.podcastDao()
            val episodeDao = appDatabase.episodeDao()

            podcastDao.insertSuspend(existingPodcast)

            // Insert an episode to be updated
            episodeDao.insertSuspend(
                PodcastEpisode(
                    uuid = updatedEpisodeUuid,
                    podcastUuid = podcastUuid,
                    title = "No title",
                    downloadUrl = "https://open.acast.com/wrong.mp3",
                    fileType = "",
                    sizeInBytes = 0,
                    duration = 0.0,
                    publishedDate = "2024-11-05T00:00:00Z".parseIsoDate() ?: Date(),
                    type = "trailer",
                ),
            )

            // Insert an episode to be deleted
            val episodeUuidToDelete = UUID.randomUUID().toString()
            // Make the added date 2 weeks ago so it can be removed
            val addedDate = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time
            episodeDao.insertSuspend(
                PodcastEpisode(
                    uuid = episodeUuidToDelete,
                    podcastUuid = podcastUuid,
                    title = "Deleted episode",
                    downloadUrl = "https://open.acast.com/delete.mp3",
                    fileType = "audio/mp3",
                    sizeInBytes = 0,
                    duration = 0.0,
                    publishedDate = Date(),
                    type = "full",
                    addedDate = addedDate,
                ),
            )

            podcastRefresher.refreshPodcast(existingPodcast = existingPodcast, playbackManager = playbackManager)

            val episodes = episodeManager.findEpisodesByPodcastOrderedByPublishDateSuspend(podcast = existingPodcast)
            assertEquals(2, episodes.size)

            val newEpisode = episodes.first()
            assertEquals(newEpisodeUuid, newEpisode.uuid)

            val updatedEpisode = episodes.last()
            assertEquals(updatedEpisodeUuid, updatedEpisode.uuid)
            assertEquals("Netflix Experimenting – DTNS 4891", updatedEpisode.title)
            assertEquals("https://open.acast.com/episode_two.mp3", updatedEpisode.downloadUrl)
            assertEquals("audio/mp3", updatedEpisode.fileType)
            assertEquals(46878457, updatedEpisode.sizeInBytes)
            assertEquals(2923.0, updatedEpisode.duration, 0.0)
            assertEquals("2024-11-06T22:00:00Z".parseIsoDate(), updatedEpisode.publishedDate)
            assertEquals("full", updatedEpisode.type)

            val newPodcast = podcastDao.findPodcastByUuidSuspend(podcastUuid)
            assertEquals("Daily Tech News Show", newPodcast?.title)
            assertEquals("Tom Merritt", newPodcast?.author)
            assertEquals("Technology\n  Tech News", newPodcast?.podcastCategory)
            assertEquals("Stay up to date with independent, authoritative and trustworthy tech news.", newPodcast?.podcastDescription)
        }
    }
}
