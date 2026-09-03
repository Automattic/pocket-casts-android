package au.com.shiftyjelly.pocketcasts.models.db

import androidx.media3.common.MimeTypes
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.AlternateEnclosureDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import com.squareup.moshi.Moshi
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlternateEnclosureDaoTest {
    private lateinit var testDb: AppDatabase
    private lateinit var alternateEnclosureDao: AlternateEnclosureDao
    private lateinit var episodeDao: EpisodeDao

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        alternateEnclosureDao = testDb.alternateEnclosureDao()
        episodeDao = testDb.episodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun replaceForEpisodeStoresSourcesAndPreservesOrder() = runTest {
        episodeDao.insertBlocking(PodcastEpisode(uuid = "episode-1", publishedDate = Date()))

        alternateEnclosureDao.replaceForEpisode(
            "episode-1",
            listOf(
                enclosure(position = 1, type = MimeTypes.APPLICATION_M3U8, uri = "https://example.com/master.m3u8"),
                enclosure(position = 0, type = "video/mp4", uri = "https://example.com/file-1080.mp4"),
            ),
        )

        val stored = alternateEnclosureDao.findByEpisodeUuid("episode-1")
        assertEquals(listOf(0, 1), stored.map { it.position })
        assertEquals(listOf("video/mp4", MimeTypes.APPLICATION_M3U8), stored.map { it.type })
        assertEquals("https://example.com/master.m3u8", stored[1].sources.single().uri)
    }

    @Test
    fun replaceForEpisodeReplacesPreviousRows() = runTest {
        episodeDao.insertBlocking(PodcastEpisode(uuid = "episode-1", publishedDate = Date()))

        alternateEnclosureDao.replaceForEpisode("episode-1", listOf(enclosure(0, "video/mp4", "https://example.com/a.mp4")))
        alternateEnclosureDao.replaceForEpisode("episode-1", listOf(enclosure(0, MimeTypes.APPLICATION_M3U8, "https://example.com/b.m3u8")))

        val stored = alternateEnclosureDao.findByEpisodeUuid("episode-1")
        assertEquals(1, stored.size)
        assertEquals(MimeTypes.APPLICATION_M3U8, stored.single().type)
    }

    @Test
    fun deletingEpisodeCascadesToEnclosures() = runTest {
        episodeDao.insertBlocking(PodcastEpisode(uuid = "episode-1", publishedDate = Date()))
        alternateEnclosureDao.replaceForEpisode("episode-1", listOf(enclosure(0, "video/mp4", "https://example.com/a.mp4")))

        episodeDao.deleteBlocking(PodcastEpisode(uuid = "episode-1", publishedDate = Date()))

        assertEquals(emptyList<EpisodeAlternateEnclosure>(), alternateEnclosureDao.findByEpisodeUuid("episode-1"))
    }

    @Test
    fun replaceForEpisodeRoundTripsMediaKind() = runTest {
        episodeDao.insertBlocking(PodcastEpisode(uuid = "episode-1", publishedDate = Date()))

        alternateEnclosureDao.replaceForEpisode(
            "episode-1",
            listOf(
                enclosure(position = 0, type = "video/mp4", uri = "https://example.com/file-1080.mp4", mediaKind = MediaKind.Video),
                enclosure(position = 1, type = MimeTypes.APPLICATION_M3U8, uri = "https://example.com/master.m3u8"),
                // A kind this version of the app doesn't know must survive the round trip so a later release can read it.
                enclosure(position = 2, type = "video/mp4", uri = "https://example.com/file-720.mp4", mediaKind = MediaKind.Unknown("hologram")),
            ),
        )

        val stored = alternateEnclosureDao.findByEpisodeUuid("episode-1")
        assertEquals(MediaKind.Video, stored[0].mediaKind)
        assertNull(stored[1].mediaKind)
        assertEquals(MediaKind.Unknown("hologram"), stored[2].mediaKind)
    }

    @Test
    fun observeByEpisodeUuidEmitsStoredEnclosuresInPositionOrder() = runTest {
        episodeDao.insertBlocking(PodcastEpisode(uuid = "video-episode", publishedDate = Date()))

        alternateEnclosureDao.replaceForEpisode(
            "video-episode",
            listOf(
                enclosure(position = 1, type = "video/mp4", uri = "https://example.com/file.mp4", mediaKind = MediaKind.Video)
                    .copy(episodeUuid = "video-episode"),
                enclosure(position = 0, type = "APPLICATION/X-MPEGURL", uri = "https://example.com/master.m3u8")
                    .copy(episodeUuid = "video-episode"),
            ),
        )

        val observed = alternateEnclosureDao.observeByEpisodeUuid("video-episode").first()
        assertEquals(listOf(0, 1), observed.map { it.position })
        assertEquals(MediaKind.Video, observed[1].mediaKind)
        assertTrue(alternateEnclosureDao.observeByEpisodeUuid("unknown-episode").first().isEmpty())
    }

    private fun enclosure(position: Int, type: String, uri: String, mediaKind: MediaKind? = null) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-1",
        position = position,
        type = type,
        mediaKind = mediaKind,
        sources = listOf(AlternateEnclosureSource(uri = uri)),
    )
}
