package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.SuggestedFoldersDao
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SuggestedFoldersManagerTest {

    private lateinit var suggestedFoldersManager: SuggestedFoldersManager

    @Mock
    private lateinit var mockPodcastCacheService: PodcastCacheServiceManager

    @Mock
    private lateinit var mockSuggestedFoldersDao: SuggestedFoldersDao

    @Mock
    private lateinit var mockAppDatabase: AppDatabase

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var hashMock: UserSetting<String>

    @Before
    fun setup() {
        whenever(mockAppDatabase.suggestedFoldersDao()).thenReturn(mockSuggestedFoldersDao)
        whenever(hashMock.value).thenReturn("123")
        whenever(settings.suggestedFoldersFollowedHash).thenReturn(hashMock)
        suggestedFoldersManager = SuggestedFoldersManager(mockPodcastCacheService, settings, mockAppDatabase)
    }

    @Test
    fun `should return a list of suggested folders`() = runBlocking {
        val folders = listOf(
            SuggestedFolder("uuid", "Folder1"),
            SuggestedFolder("uuid2", "Folder2"),
        )

        whenever(mockSuggestedFoldersDao.findAll()).thenReturn(flowOf(folders))

        val result = suggestedFoldersManager.getSuggestedFolders().first()

        assertEquals(folders, result)
    }

    @Test
    fun `should insert folders into database when server returns folders`() = runBlocking {
        val podcastUuids = listOf("podcastUuid")
        val folders = listOf(
            SuggestedFolder("uuid", "Folder1"),
        )

        whenever(mockPodcastCacheService.suggestedFolders(any())).thenReturn(folders)

        suggestedFoldersManager.refreshSuggestedFolders(podcastUuids)

        verify(mockSuggestedFoldersDao).deleteAndInsertAll(folders)
    }

    @Test
    fun `should not insert folders into database if hash did not change`() = runBlocking {
        val podcastUuids = listOf("podcastUuid")

        whenever(hashMock.value).thenReturn("different-hash")
        whenever(settings.suggestedFoldersFollowedHash).thenReturn(hashMock)

        suggestedFoldersManager.refreshSuggestedFolders(podcastUuids)

        verify(mockSuggestedFoldersDao, times(0)).deleteAndInsertAll(any())
    }

    @Test
    fun `should not insert folders into database when service throws an exception`() = runBlocking {
        val podcastUuids = listOf("uuid1", "uuid2")

        whenever(mockPodcastCacheService.suggestedFolders(any())).thenThrow(RuntimeException("Test Exception"))

        suggestedFoldersManager.refreshSuggestedFolders(podcastUuids)

        verify(mockSuggestedFoldersDao, times(0)).deleteAndInsertAll(any())
    }

    @Test
    fun `should delete suggested folders`() = runBlocking {
        val folders = listOf(
            SuggestedFolder("uuid", "Folder1"),
        )

        suggestedFoldersManager.replaceSuggestedFolders(folders)

        verify(mockSuggestedFoldersDao).deleteFolders(folders)
    }
}
