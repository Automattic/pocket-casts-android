package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    lateinit var userEpisode: UserEpisode

    private lateinit var userEpisodeManagerImpl: UserEpisodeManagerImpl

    @Before
    fun setUp() = runTest {
        whenever(appDatabase.userEpisodeDao()).thenReturn(userEpisodeDao)
        userEpisodeManagerImpl = UserEpisodeManagerImpl(
            appDatabase = appDatabase,
            settings = mock(),
            syncManager = mock(),
            downloadManager = mock(),
            context = mock(),
            subscriptionManager = mock(),
            episodeAnalytics = mock(),
        )
    }

    @Test
    fun `select chapter removes element`() = runTest {
        whenever(userEpisode.deselectedChapters).thenReturn(ChapterIndices(listOf(1, 2, 3)))

        userEpisodeManagerImpl.selectChapterIndexForEpisode(1, userEpisode)

        verify(userEpisode).deselectedChapters = ChapterIndices(listOf(2, 3))
    }

    @Test
    fun `deselect chapter adds element`() = runTest {
        whenever(userEpisode.deselectedChapters).thenReturn(ChapterIndices(listOf(1, 2)))

        userEpisodeManagerImpl.deselectChapterIndexForEpisode(3, userEpisode)

        verify(userEpisode).deselectedChapters = ChapterIndices(listOf(1, 2, 3))
    }

    @Test
    fun `deselect chapter is not added twice`() = runTest {
        whenever(userEpisode.deselectedChapters).thenReturn(ChapterIndices(listOf(1, 2, 3)))

        userEpisodeManagerImpl.deselectChapterIndexForEpisode(3, userEpisode)

        verify(userEpisodeDao, never()).update(any())
    }
}
