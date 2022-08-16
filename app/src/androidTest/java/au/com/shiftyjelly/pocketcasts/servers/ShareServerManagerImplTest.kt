package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.list.ListServerManagerImpl
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.HttpURLConnection
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

class ShareServerManagerImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var retrofit: Retrofit

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .client(client)
            .build()
    }

    @Test
    fun buildSecurityHash() {
        val hash = ListServerManagerImpl.buildSecurityHash(date = "20160913144421", serverSecret = "APP_SERVER_SECRET")
        assertEquals("f91e649642ffb53813a79f816f2ef1ac36a52c4e", hash)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun createPodcastListTest() {
        val responseBody = """
            {
              "status":"ok",
              "result":
                {
                  "share_url":"https://static.pocketcasts.com/share/list/02dcb703-e8b4-4d3d-9cc7-ed7ec8dea2fd.html"
                }
            }
        """
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(responseBody)
        mockWebServer.enqueue(response)

        val instant = LocalDateTime.parse("2021-11-25T15:20").atZone(ZoneId.systemDefault()).toInstant()

        runTest {
            val url = ListServerManagerImpl(uploadRetrofit = retrofit, downloadRetrofit = retrofit)
                .createPodcastList(
                    title = "A fun title",
                    description = "A even funnier description",
                    date = Date.from(instant),
                    podcasts = listOf(
                        Podcast(uuid = "77f1b350-5419-0134-ec2d-0d50f522381b", title = "Heavyweight", author = "Gimlet"),
                        Podcast(uuid = "30817be0-5675-0134-ec37-0d50f522381b", title = "Still Processing", author = "The New York Times"),
                        Podcast(uuid = "ac371bd0-094f-0134-9ce1-59d98c6b72b8", title = "Revisionist History", author = "Malcolm Gladwell / Panoply")
                    ),
                    serverSecret = "APP_SERVER_SECRET"
                )

            val request = mockWebServer.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("application/json; charset=UTF-8", request.getHeader("Content-Type"))
            JSONAssert.assertEquals(
                """
                    {
                        "title":"A fun title",
                        "description":"A even funnier description",
                        "datetime":"20211125152000",
                        "h":"0dc79dc94a3acea0618823a2a67b5a9a0ff5bbc9",
                        "podcasts":
                            [
                                {"uuid":"77f1b350-5419-0134-ec2d-0d50f522381b","title":"Heavyweight","author":"Gimlet"},
                                {"uuid":"30817be0-5675-0134-ec37-0d50f522381b","title":"Still Processing","author":"The New York Times"},
                                {"uuid":"ac371bd0-094f-0134-9ce1-59d98c6b72b8","title":"Revisionist History","author":"Malcolm Gladwell / Panoply"}
                            ]
                    }
                """.trimIndent(),
                request.body.readUtf8(),
                false
            )

            assertEquals("https://static.pocketcasts.com/share/list/02dcb703-e8b4-4d3d-9cc7-ed7ec8dea2fd.html", url)
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun openPodcastListTest() {
        val responseBody = """
            {
                "title":"Android Development",
                "description":"My amazing description!",
                "podcasts":
                    [
                        {"uuid":"976c2fc0-1c0e-012e-00fd-00163e1b201c","title":"Android Central Podcast","author":"Android Central"},
                        {"uuid":"b8f83d70-adf5-0133-2e33-6dc413d6d41d","title":"The Context #androiddev","author":"Artem Zinnatullin"},
                        {"uuid":"86f03de0-ed6b-0132-11d1-059c869cc4eb","title":"The Blerg","author":"Chris Lacy"},
                        {"uuid":"bc824bd0-3c75-012e-13a6-00163e1b201c","title":"All About Android (MP3)","author":"TWiT"}
                    ]
                }
        """
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(responseBody)
        mockWebServer.enqueue(response)

        runTest {
            val podcastList = ListServerManagerImpl(uploadRetrofit = retrofit, downloadRetrofit = retrofit)
                .openPodcastList(listId = "85c4bc9c-d905-40b1-96de-50c197803e4b")

            assertEquals("Android Development", podcastList.title)
            assertEquals("My amazing description!", podcastList.description)
            assertEquals(4, podcastList.podcasts.size)
            val podcast = podcastList.fullPodcasts[2]
            assertEquals("86f03de0-ed6b-0132-11d1-059c869cc4eb", podcast.uuid)
            assertEquals("The Blerg", podcast.title)
            assertEquals("Chris Lacy", podcast.author)
        }
    }

    @Test
    fun extractShareListIdFromWebUrl() {
        var websiteUrl = "${BuildConfig.SERVER_LIST_URL}/981d8691-5e1f-4009-adf4-ee5a204bd00c"
        var id = ListServerManagerImpl.extractShareListIdFromWebUrl(websiteUrl)
        assertEquals("981d8691-5e1f-4009-adf4-ee5a204bd00c", id)

        websiteUrl = "/${BuildConfig.SERVER_LIST_HOST}/fddf52be-6058-48c4-a821-09edc8ad023d"
        id = ListServerManagerImpl.extractShareListIdFromWebUrl(websiteUrl)
        assertEquals("fddf52be-6058-48c4-a821-09edc8ad023d", id)
    }
}
