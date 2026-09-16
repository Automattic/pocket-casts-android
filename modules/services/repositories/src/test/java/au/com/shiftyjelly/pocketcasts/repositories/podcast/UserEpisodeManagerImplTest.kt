package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
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
import org.mockito.kotlin.mock
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

    private lateinit var userEpisodeManagerImpl: UserEpisodeManagerImpl

    @Before
    fun setUp() = runTest {
        whenever(appDatabase.userEpisodeDao()).thenReturn(userEpisodeDao)
        userEpisodeManagerImpl = UserEpisodeManagerImpl(
            appDatabase = appDatabase,
            settings = mock(),
            syncManager = mock(),
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
}
