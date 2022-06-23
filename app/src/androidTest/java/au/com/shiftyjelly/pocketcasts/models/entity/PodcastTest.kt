package au.com.shiftyjelly.pocketcasts.models.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastTest {

    @Test
    fun getShortUrl() {
        assertEquals("sequelpitch.co.uk", podcastWithWebsite("HTTPS://sequelpitch.co.uk").getShortUrl())
        assertEquals("abc.net.au", podcastWithWebsite("https://www.abc.net.au/radionational/programs/partyroom/").getShortUrl())
        assertEquals("nytimes.com", podcastWithWebsite("https://www.nytimes.com/the-daily").getShortUrl())
        assertEquals("bbc.co.uk", podcastWithWebsite("http://www.bbc.co.uk/programmes/p0bqztzm").getShortUrl())
        assertEquals("", podcastWithWebsite("http://null").getShortUrl())
    }

    private fun podcastWithWebsite(website: String): Podcast {
        return Podcast().apply {
            podcastUrl = website
        }
    }
}
