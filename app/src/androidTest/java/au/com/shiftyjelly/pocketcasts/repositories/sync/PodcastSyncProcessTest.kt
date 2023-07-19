package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Retrofit
import timber.log.Timber
import java.net.HttpURLConnection
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PodcastSyncProcessTest {

    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var retrofit: Retrofit
    private lateinit var okhttpCache: Cache
    private lateinit var appDatabase: AppDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        mockWebServer = MockWebServer()
        mockWebServer.start()

        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        val moshi = ServersModule.provideMoshiBuilder().build()
        val okHttpClient = OkHttpClient.Builder().build()
        retrofit = ServersModule.provideRetrofit(baseUrl = mockWebServer.url("/").toString(), okHttpClient = okHttpClient, moshi = moshi)
        okhttpCache = ServersModule.provideCache(folder = "TestCache", context = context)
    }

    /**
     * Test the sync process handles bookmarks.
     */
    @Test
    fun bookmarkSync() {
        runTest {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            val podcastManager: PodcastManager = mock()
            whenever(podcastManager.findPodcastsToSync()).thenReturn(emptyList())

            val episodeManager: EpisodeManager = mock()
            whenever(episodeManager.findEpisodesToSync()).thenReturn(emptyList())

            val playlistManager: PlaylistManager = mock()
            whenever(playlistManager.findPlaylistsToSync()).thenReturn(emptyList())

            val folderManager: FolderManager = mock()
            whenever(folderManager.findFoldersToSync()).thenReturn(emptyList())

            val bookmarkManager = BookmarkManagerImpl(appDatabase = appDatabase)
            val bookmarkToUpdate = bookmarkManager.add(
                episode = PodcastEpisode(
                    uuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb",
                    podcastUuid = "3f580d2e-d9c0-4cde-94b3-728c271f373a",
                    publishedDate = Date()
                ),
                timeSecs = 23,
                title = "Bookmark"
            )
            val bookmarkToDelete = bookmarkManager.add(
                episode = PodcastEpisode(
                    uuid = "920cbb66-d5dc-4128-a2a0-c8bfbe55ce78",
                    podcastUuid = "3fcb9f78-24a0-49b9-9078-8f572280b61d",
                    publishedDate = Date()
                ),
                timeSecs = 875,
                title = "Bookmark Deleted"
            )
            val bookmarkUuidToAdd = "d0f337c2-4d85-40b7-ae59-893a75fe42bc"

            val statsManager: StatsManager = mock()
            whenever(statsManager.isEmpty) doReturn true

            val settings: Settings = mock()
            whenever(settings.getUniqueDeviceId()) doReturn "unique_device_id"
            whenever(settings.getVersion()) doReturn "1.0"
            whenever(settings.getVersionCode()) doReturn 1

            val syncAccountManager: SyncAccountManager = mock()
            whenever(syncAccountManager.getEmail()) doReturn "noreply@pocketcasts.com"
            whenever(syncAccountManager.isLoggedIn()) doReturn true
            whenever(syncAccountManager.getAccessToken()) doReturn AccessToken("access_token")

            val syncServerManager = SyncServerManager(
                retrofit = retrofit,
                settings = settings,
                cache = okhttpCache
            )

            val syncManager = SyncManagerImpl(
                analyticsTracker = mock(),
                context = context,
                settings = settings,
                syncAccountManager = syncAccountManager,
                syncServerManager = syncServerManager
            )

            val syncProcess = PodcastSyncProcess(
                context = context,
                settings = settings,
                episodeManager = episodeManager,
                podcastManager = podcastManager,
                playlistManager = playlistManager,
                bookmarkManager = bookmarkManager,
                statsManager = statsManager,
                fileStorage = mock(),
                playbackManager = mock(),
                podcastCacheServerManager = mock(),
                userEpisodeManager = mock(),
                subscriptionManager = mock(),
                folderManager = folderManager,
                syncManager = syncManager
            )

            val response = MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {
                      "status": "ok",
                      "result": {
                        "last_modified": "2023-07-06T23:04:04.115Z",
                        "changes": [
                          {
                            "type": "UserBookmark",
                            "fields": {
                              "bookmark_uuid": "${bookmarkToUpdate.uuid}",
                              "podcast_uuid": "${bookmarkToUpdate.podcastUuid}",
                              "episode_uuid": "${bookmarkToUpdate.episodeUuid}",
                              "created_at": "2023-07-06T23:01:55Z",
                              "time": "23",
                              "is_deleted": false,
                              "is_deleted_modified": "1688684515849",
                              "title": "Bookmark Title Updated",
                              "title_modified": "1688684515849"
                            }
                          },
                          {
                            "type": "UserBookmark",
                            "fields": {
                              "bookmark_uuid": "$bookmarkUuidToAdd",
                              "podcast_uuid": "e979cf2f-58f2-4f47-8ad7-b9b58d511346",
                              "episode_uuid": "b70fcdf2-dc04-44b1-8829-1028374fc656",
                              "created_at": "2023-06-16T02:04:35Z",
                              "time": "875",
                              "is_deleted": false,
                              "is_deleted_modified": "1688684515849",
                              "title": "Bookmark Added",
                              "title_modified": "1688684515849"
                            }
                          },
                          {
                            "type": "UserBookmark",
                            "fields": {
                              "bookmark_uuid": "${bookmarkToDelete.uuid}",
                              "podcast_uuid": "${bookmarkToDelete.podcastUuid}",
                              "episode_uuid": "${bookmarkToDelete.episodeUuid}",
                              "created_at": "2023-06-12T01:01:56Z",
                              "time": "1050",
                              "is_deleted": true,
                              "is_deleted_modified": "1688684515849",
                              "title": "Bookmark Deleted",
                              "title_modified": "1688684515849"
                            }
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
            mockWebServer.enqueue(response)

            val lastModified = System.currentTimeMillis().toString()

            syncProcess.performIncrementalSync(lastModified)
                .doOnError {
                    Timber.e(it)
                }
                .test()
                .awaitDone(5, TimeUnit.SECONDS)

            val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
            assertEquals("/sync/update", request?.path)

            // check an existing bookmark is updated
            val updatedBookmark = bookmarkManager.findBookmark(bookmarkToUpdate.uuid)
            assertEquals("Bookmark Title Updated", updatedBookmark?.title)
            // check new bookmarks are added
            val newBookmark = bookmarkManager.findBookmark(bookmarkUuidToAdd)
            assertNotNull("Bookmark should have been added", newBookmark)
            assertEquals("New bookmark podcast UUID should match", "e979cf2f-58f2-4f47-8ad7-b9b58d511346", newBookmark?.podcastUuid)
            assertEquals("New bookmark episode UUID should match", "b70fcdf2-dc04-44b1-8829-1028374fc656", newBookmark?.episodeUuid)
            assertEquals("2023-06-16T02:04:35Z", newBookmark?.createdAt?.toIsoString())
            assertEquals("Bookmark Added", newBookmark?.title)
            assertEquals(875, newBookmark?.timeSecs)
            // check deleted bookmarks are removed
            assertNull("Bookmark should of been deleted", bookmarkManager.findBookmark(bookmarkToDelete.uuid))
        }
    }
}
