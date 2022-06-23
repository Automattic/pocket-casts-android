package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class ShareServerManagerImplTest {

    @Before
    @Throws(Exception::class)
    fun setUp() {
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    @Throws(Exception::class)
    fun buildSecurityHash() {
        val hash = ShareServerManagerImpl.buildSecurityHash("20160913144421")
        assertThat<String>(hash, `is`(equalTo("fe6cec0d000c6d087d8f2c83912d5631b3161837")))
    }

    @Test
    @Throws(Exception::class)
    fun sharePodcastListToJson() {
        val podcasts = ArrayList<Podcast>()
        podcasts.add(addPodcast("77f1b350-5419-0134-ec2d-0d50f522381b", "Heavyweight", "Gimlet"))
        podcasts.add(addPodcast("30817be0-5675-0134-ec37-0d50f522381b", "Still Processing", "The New York Times"))
        podcasts.add(addPodcast("ac371bd0-094f-0134-9ce1-59d98c6b72b8", "Revisionist History", "Malcolm Gladwell / Panoply"))
        val date = "20160913105900"
        val hash = "d7f4973f38aba90f13390764bed4bf44e557afde"
        val json = ShareServerManagerImpl.sharePodcastListToJson("A fun title", "A even funnier description", podcasts, date, hash)
        println(json)
        JSONAssert.assertEquals(
            "{" +
                "\"podcasts\":[" +
                "{\"title\":\"Heavyweight\",\"author\":\"Gimlet\",\"uuid\":\"77f1b350-5419-0134-ec2d-0d50f522381b\"}," +
                "{\"title\":\"Still Processing\",\"author\":\"The New York Times\",\"uuid\":\"30817be0-5675-0134-ec37-0d50f522381b\"}," +
                "{\"title\":\"Revisionist History\",\"author\":\"Malcolm Gladwell / Panoply\",\"uuid\":\"ac371bd0-094f-0134-9ce1-59d98c6b72b8\"}" +
                "]," +
                "\"title\":\"A fun title\"," +
                "\"description\":\"A even funnier description\"," +
                "\"datetime\":\"20160913105900\"," +
                "\"h\":\"d7f4973f38aba90f13390764bed4bf44e557afde\"}",
            json,
            false
        )
    }

    @Test
    @Throws(Exception::class)
    fun parseCreatePodcastListResponse() {
        val response = "{\"status\":\"ok\",\"result\":{\"share_url\":\"https://static.pocketcasts.com/share/list/02dcb703-e8b4-4d3d-9cc7-ed7ec8dea2fd.html\"}}"
        val url = ShareServerManagerImpl.parseCreatePodcastListResponse(response)
        assertThat<String>(url, `is`(equalTo("https://static.pocketcasts.com/share/list/02dcb703-e8b4-4d3d-9cc7-ed7ec8dea2fd.html")))
    }

    @Test
    @Throws(Exception::class)
    fun parseLoadPodcastListResponse() {
        val jsonResponse = "{\"title\":\"Android Development\",\"description\":\"My amazing description!\"," +
            "\"podcasts\":[" +
            "{\"uuid\":\"976c2fc0-1c0e-012e-00fd-00163e1b201c\",\"title\":\"Android Central Podcast\",\"author\":\"Android Central\"}," +
            "{\"uuid\":\"b8f83d70-adf5-0133-2e33-6dc413d6d41d\",\"title\":\"The Context #androiddev\",\"author\":\"Artem Zinnatullin\"}," +
            "{\"uuid\":\"86f03de0-ed6b-0132-11d1-059c869cc4eb\",\"title\":\"The Blerg\",\"author\":\"Chris Lacy\"}," +
            "{\"uuid\":\"bc824bd0-3c75-012e-13a6-00163e1b201c\",\"title\":\"All About Android (MP3)\",\"author\":\"TWiT\"}" +
            "]}"
        val response = ShareServerManagerImpl.parseLoadPodcastListResponse(jsonResponse)
        assertThat<ShareServerManagerImpl.PodcastListResponse>(response, `is`(notNullValue()))
        response ?: return
        assertThat<String>(response.title, `is`(equalTo("Android Development")))
        assertThat<String>(response.description, `is`(equalTo("My amazing description!")))
        // TODO
        // assertThat(response.getPodcasts(), hasSize(4));
        assertThat<String>(response.podcasts[2].uuid, `is`(equalTo("86f03de0-ed6b-0132-11d1-059c869cc4eb")))
        assertThat(response.podcasts[2].title, `is`(equalTo("The Blerg")))
        assertThat(response.podcasts[2].author, `is`(equalTo("Chris Lacy")))
    }

    private fun addPodcast(uuid: String, title: String, author: String): Podcast {
        val podcast = Podcast()
        podcast.uuid = uuid
        podcast.title = title
        podcast.author = author
        return podcast
    }

    @Test
    @Throws(Exception::class)
    fun extractShareListIdFromWebUrl() {
        var websiteUrl = "${BuildConfig.SERVER_LIST_URL}/981d8691-5e1f-4009-adf4-ee5a204bd00c"
        var id = ShareServerManagerImpl.extractShareListIdFromWebUrl(websiteUrl)
        assertThat<String>(id, `is`(equalTo("981d8691-5e1f-4009-adf4-ee5a204bd00c")))

        websiteUrl = "/${BuildConfig.SERVER_LIST_HOST}/fddf52be-6058-48c4-a821-09edc8ad023d"
        id = ShareServerManagerImpl.extractShareListIdFromWebUrl(websiteUrl)
        assertThat<String>(id, `is`(equalTo("fddf52be-6058-48c4-a821-09edc8ad023d")))
    }
}
