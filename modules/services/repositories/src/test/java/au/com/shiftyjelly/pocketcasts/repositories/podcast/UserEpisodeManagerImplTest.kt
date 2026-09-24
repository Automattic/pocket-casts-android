package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.ServerFile
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class UserEpisodeManagerImplTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var appDatabase: AppDatabase

    @Mock
    lateinit var userEpisodeDao: UserEpisodeDao

    @Mock
    lateinit var syncManager: SyncManager

    private lateinit var userEpisodeManagerImpl: UserEpisodeManagerImpl

    @Before
    fun setUp() = runTest {
        whenever(appDatabase.userEpisodeDao()).thenReturn(userEpisodeDao)
        userEpisodeManagerImpl = UserEpisodeManagerImpl(
            appDatabase = appDatabase,
            settings = mock(),
            syncManager = syncManager,
            downloadQueue = mock(),
            context = mock(),
            eventHorizon = EventHorizon(TestEventSink()),
        )
    }

    @Test
    fun `select chapter removes element`() = runTest {
        val userEpisode = UserEpisode(
            uuid = "uuid",
            publishedDate = Date(),
            deselectedChapters = ChapterIndices(listOf(1, 2, 3)),
        )

        userEpisodeManagerImpl.selectChapterIndexForEpisode(1, userEpisode)

        assertEquals(ChapterIndices(listOf(2, 3)), userEpisode.deselectedChapters)
    }

    @Test
    fun `deselect chapter adds element`() = runTest {
        val userEpisode = UserEpisode(
            uuid = "uuid",
            publishedDate = Date(),
            deselectedChapters = ChapterIndices(listOf(1, 2)),
        )

        userEpisodeManagerImpl.deselectChapterIndexForEpisode(3, userEpisode)

        assertEquals(ChapterIndices(listOf(1, 2, 3)), userEpisode.deselectedChapters)
    }

    @Test
    fun `deselect chapter is not added twice`() = runTest {
        val userEpisode = UserEpisode(
            uuid = "uuid",
            publishedDate = Date(),
            deselectedChapters = ChapterIndices(listOf(1, 2, 3)),
        )

        userEpisodeManagerImpl.deselectChapterIndexForEpisode(3, userEpisode)

        assertEquals(ChapterIndices(listOf(1, 2, 3)), userEpisode.deselectedChapters)
    }

    @Test
    fun `episode flowable skips emissions while the episode does not exist`() = runTest {
        val userEpisode = UserEpisode(uuid = "uuid", publishedDate = Date())
        whenever(userEpisodeDao.findEpisodeFlow("uuid")).thenReturn(flowOf(null, userEpisode, null))

        val emissions = userEpisodeManagerImpl.episodeRxFlowable("uuid").asFlow().toList()

        assertEquals(listOf(userEpisode), emissions)
    }

    @Test
    fun `download missing user episode keeps an episode that is already on the server`() = runTest {
        val userEpisode = UserEpisode(uuid = "uuid", publishedDate = Date(), serverStatus = UserEpisodeServerStatus.UPLOADED)
        whenever(userEpisodeDao.findEpisodeByUuid("uuid")).thenReturn(userEpisode)

        val episode = userEpisodeManagerImpl.downloadMissingUserEpisode("uuid", placeholderTitle = null, placeholderPublished = null)

        assertEquals(userEpisode, episode)
        verify(userEpisodeDao, never()).insertOrReplace(any())
    }

    @Test
    fun `download missing user episode replaces a missing episode with the server copy`() = runTest {
        val missingEpisode = UserEpisode(uuid = "uuid", publishedDate = Date(), serverStatus = UserEpisodeServerStatus.MISSING)
        whenever(userEpisodeDao.findEpisodeByUuid("uuid")).thenReturn(missingEpisode)
        whenever(syncManager.getUserEpisode("uuid")).thenReturn(serverFile("uuid", "Server title"))

        userEpisodeManagerImpl.downloadMissingUserEpisode("uuid", placeholderTitle = "Placeholder", placeholderPublished = null)

        verify(userEpisodeDao).insertOrReplace(argThat { title == "Server title" && serverStatus == UserEpisodeServerStatus.UPLOADED })
    }

    @Test
    fun `download missing user episode substitutes a placeholder when the server does not have the file`() = runTest {
        val missingEpisode = UserEpisode(uuid = "uuid", publishedDate = Date(), serverStatus = UserEpisodeServerStatus.MISSING)
        whenever(userEpisodeDao.findEpisodeByUuid("uuid")).thenReturn(missingEpisode)
        whenever(syncManager.getUserEpisode("uuid")).thenReturn(null)

        userEpisodeManagerImpl.downloadMissingUserEpisode("uuid", placeholderTitle = "Placeholder", placeholderPublished = null)

        verify(userEpisodeDao).insertOrReplace(argThat { uuid == "uuid" && title == "Placeholder" && serverStatus == UserEpisodeServerStatus.MISSING })
    }

    @Test
    fun `download missing user episode writes nothing when the server request fails`() = runTest {
        whenever(userEpisodeDao.findEpisodeByUuid("uuid")).thenReturn(null)
        whenever(syncManager.getUserEpisode("uuid")).thenThrow(RuntimeException("Server unavailable"))

        val failure = runCatching {
            userEpisodeManagerImpl.downloadMissingUserEpisode("uuid", placeholderTitle = null, placeholderPublished = null)
        }.exceptionOrNull()

        assertEquals("Server unavailable", failure?.message)
        verify(userEpisodeDao, never()).insertOrReplace(any())
    }

    private fun serverFile(uuid: String, title: String) = ServerFile(
        uuid = uuid,
        colour = 0,
        contentType = "audio/mp3",
        duration = 100,
        hasCustomImage = false,
        imageUrl = "imageUrl",
        playedUpTo = 0,
        playedUpToModified = 0,
        playingStatus = EpisodePlayingStatus.NOT_PLAYED,
        playingStatusModified = 0,
        publishedDate = Date(),
        size = 1000,
        title = title,
    )
}
