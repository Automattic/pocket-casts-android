package au.com.shiftyjelly.pocketcasts.repositories.images

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastImageTest {

    private val testUuid = "test-podcast-uuid"

    @Test
    fun `getMediumArtworkUrl returns 480 image`() {
        val url = PodcastImage.getMediumArtworkUrl(testUuid)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/480/test-podcast-uuid.webp", url)
    }

    @Test
    fun `getArtworkUrl with null size returns 960 image`() {
        val url = PodcastImage.getArtworkUrl(size = null, uuid = testUuid, isWearOS = false)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/960/$testUuid.webp", url)
    }

    @Test
    fun `getArtworkUrl with size greater than 480 returns 960 image size`() {
        val url = PodcastImage.getArtworkUrl(size = 800, uuid = testUuid, isWearOS = false)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/960/test-podcast-uuid.webp", url)
    }

    @Test
    fun `getArtworkUrl with size greater than 200 and 480 returns 480 size`() {
        val urlAt201 = PodcastImage.getArtworkUrl(size = 201, uuid = testUuid, isWearOS = false)
        val urlAt300 = PodcastImage.getArtworkUrl(size = 300, uuid = testUuid, isWearOS = false)
        val urlAt480 = PodcastImage.getArtworkUrl(size = 480, uuid = testUuid, isWearOS = false)

        val expectedUrl = "${Settings.SERVER_STATIC_URL}/discover/images/webp/480/test-podcast-uuid.webp"
        assertEquals(expectedUrl, urlAt201)
        assertEquals(expectedUrl, urlAt300)
        assertEquals(expectedUrl, urlAt480)
    }

    @Test
    fun `getArtworkUrl with size 200 or less returns small size`() {
        val urlAt200 = PodcastImage.getArtworkUrl(size = 200, uuid = testUuid, isWearOS = false)
        val urlAt100 = PodcastImage.getArtworkUrl(size = 100, uuid = testUuid, isWearOS = false)

        val expectedUrl = "${Settings.SERVER_STATIC_URL}/discover/images/webp/200/test-podcast-uuid.webp"
        assertEquals(expectedUrl, urlAt200)
        assertEquals(expectedUrl, urlAt100)
    }

    @Test
    fun `getArtworkUrl with null size returns 480 image for WearOS`() {
        val url = PodcastImage.getArtworkUrl(size = null, uuid = testUuid, isWearOS = true)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/480/$testUuid.webp", url)
    }

    @Test
    fun `getArtworkUrl with size greater than 480 returns 480 image size for WearOS`() {
        val url = PodcastImage.getArtworkUrl(size = 800, uuid = testUuid, isWearOS = true)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/480/$testUuid.webp", url)
    }

    @Test
    fun `getArtworkUrls for regular device returns three sizes`() {
        val urls = PodcastImage.getArtworkUrls(uuid = testUuid, isWearOS = false)

        assertEquals(3, urls.size)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/960/$testUuid.webp", urls[0])
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/480/$testUuid.webp", urls[1])
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/200/$testUuid.webp", urls[2])
    }

    @Test
    fun `getArtworkUrls for WearOS returns two sizes without large`() {
        val urls = PodcastImage.getArtworkUrls(uuid = testUuid, isWearOS = true)

        assertEquals(2, urls.size)
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/480/$testUuid.webp", urls[0])
        assertEquals("${Settings.SERVER_STATIC_URL}/discover/images/webp/200/$testUuid.webp", urls[1])
    }
}
