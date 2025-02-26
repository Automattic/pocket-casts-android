package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.SuggestedFoldersDao
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.SuggestedFoldersRequest
import au.com.shiftyjelly.pocketcasts.utils.LocaleUtil
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SuggestedFoldersManagerTest {

    private lateinit var suggestedFoldersManager: SuggestedFoldersManager
    private val mockPodcastCacheService: PodcastCacheServiceManager = mock<PodcastCacheServiceManager>()
    private val mockAppDatabase: AppDatabase = mock<AppDatabase>()
    private val mockSuggestedFoldersDao: SuggestedFoldersDao = mock<SuggestedFoldersDao>()
    private val localeUtil: LocaleUtil = object : LocaleUtil {
        override fun getLanguage(): String = "language"
    }
    private val emptyLocaleUtil: LocaleUtil = object : LocaleUtil {
        override fun getLanguage(): String = ""
    }

    @Before
    fun setup() {
        whenever(mockAppDatabase.suggestedFoldersDao()).thenReturn(mockSuggestedFoldersDao)
        suggestedFoldersManager = SuggestedFoldersManager(mockPodcastCacheService, localeUtil, mockAppDatabase)
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

        whenever(
            mockPodcastCacheService.suggestedFolders(eq(SuggestedFoldersRequest(podcastUuids, "language"))),
        ).thenReturn(folders)

        suggestedFoldersManager.refreshSuggestedFolders(podcastUuids)

        verify(mockSuggestedFoldersDao).deleteAndInsertAll(folders)
    }

    @Test
    fun `should insert folders into database with default language`() = runBlocking {
        val podcastUuids = listOf("podcastUuid")
        val folders = listOf(
            SuggestedFolder("uuid", "Folder1"),
        )

        suggestedFoldersManager = SuggestedFoldersManager(mockPodcastCacheService, emptyLocaleUtil, mockAppDatabase)

        whenever(
            mockPodcastCacheService.suggestedFolders(eq(SuggestedFoldersRequest(podcastUuids, "en"))),
        ).thenReturn(folders)

        suggestedFoldersManager.refreshSuggestedFolders(podcastUuids)

        verify(mockSuggestedFoldersDao).deleteAndInsertAll(folders)
    }

    @Test
    fun `should not insert folders into database when service throws an exception`() = runBlocking {
        val podcastUuids = listOf("uuid1", "uuid2")
        whenever(mockPodcastCacheService.suggestedFolders(any())).thenThrow(RuntimeException("Test Exception"))

        suggestedFoldersManager.refreshSuggestedFolders(podcastUuids)

        verify(mockSuggestedFoldersDao, times(0)).insertAll(any())
    }

    @Test
    fun `should delete suggested folders`() = runBlocking {
        val folders = listOf(
            SuggestedFolder("uuid", "Folder1"),
        )

        suggestedFoldersManager.deleteSuggestedFolders(folders)

        verify(mockSuggestedFoldersDao).deleteFolders(folders)
    }
}
