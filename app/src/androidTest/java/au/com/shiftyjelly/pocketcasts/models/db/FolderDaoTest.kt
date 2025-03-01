package au.com.shiftyjelly.pocketcasts.models.db

import android.graphics.Color
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.FolderDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.utils.FakeFileGenerator.fakeFolder
import com.squareup.moshi.Moshi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderDaoTest {
    private lateinit var folderDao: FolderDao
    private lateinit var testDatabase: AppDatabase

    @Before
    fun setupDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        folderDao = testDatabase.folderDao()
    }

    @After
    fun closeDatabase() {
        testDatabase.clearAllTables()
        testDatabase.close()
    }

    @Test
    fun testInsertFolderShouldStoreItCorrectly() = runTest {
        folderDao.insert(fakeFolder)
        val foundFolder = folderDao.findByUuid(fakeFolder.uuid)
        assertNotNull(foundFolder)
        assertEquals(fakeFolder.uuid, foundFolder?.uuid)
        assertEquals(fakeFolder.name, foundFolder?.name)
    }

    @Test
    fun testInsertAllFoldersShouldStoreItCorrectly() = runTest {
        folderDao.insertAll(listOf(fakeFolder.copy(uuid = "uuid1"), fakeFolder.copy(uuid = "uuid2")))

        val foundFolder1 = folderDao.findByUuid("uuid1")
        assertEquals(foundFolder1?.uuid, "uuid1")

        val foundFolder2 = folderDao.findByUuid("uuid2")
        assertEquals(foundFolder2?.uuid, "uuid2")
    }

    @Test
    fun deleteAllShouldRemoveAllFolders() = runTest {
        folderDao.insert(fakeFolder)
        folderDao.deleteAll()
        val foundFolders = folderDao.findFolders()
        assertTrue(foundFolders.isEmpty())
    }

    @Test
    fun findByUuidShouldReturnFolderWithMatchingUUID() = runTest {
        folderDao.insert(fakeFolder)
        val foundFolder = folderDao.findByUuid(fakeFolder.uuid)
        assertNotNull(foundFolder)
        assertEquals(fakeFolder.uuid, foundFolder?.uuid)
        assertEquals(fakeFolder.name, foundFolder?.name)
    }

    @Test
    fun findByUuidShouldReturnNullWhenUUIDDoesNotExist() = runTest {
        val foundFolder = folderDao.findByUuid("non_existing_uuid")
        assertNull(foundFolder)
    }

    @Test
    fun updateSortPositionShouldUpdateTheSortPositionOfTheFolder() = runTest {
        folderDao.insert(fakeFolder)

        val newSortPosition = 2
        val syncModified = System.currentTimeMillis()
        folderDao.updateSortPosition(newSortPosition, fakeFolder.uuid, syncModified)

        val updatedFolder = folderDao.findByUuid(fakeFolder.uuid)
        assertNotNull(updatedFolder)
        assertEquals(newSortPosition, updatedFolder?.sortPosition)
        assertEquals(syncModified, updatedFolder?.syncModified)
    }

    @Test
    fun updateFolderSortTypeShouldUpdateTheSortTypeOfTheFolder() = runTest {
        folderDao.insert(fakeFolder)

        val newSortType = PodcastsSortType.NAME_A_TO_Z
        val syncModified = System.currentTimeMillis()
        folderDao.updateFolderSortType(fakeFolder.uuid, newSortType, syncModified)

        val updatedFolder = folderDao.findByUuid(fakeFolder.uuid)
        assertNotNull(updatedFolder)
        assertEquals(newSortType, updatedFolder?.podcastsSortType)
        assertEquals(syncModified, updatedFolder?.syncModified)
    }

    @Test
    fun updateAllSyncedShouldUpdateTheSynModifiedFieldOfAllFolders() = runTest {
        folderDao.insert(fakeFolder)
        folderDao.updateAllSynced()
        val updatedFolder = folderDao.findFolders()
        assertTrue(updatedFolder.all { it.syncModified == 0L })
    }

    @Test
    fun updateFolderColorShouldUpdateTheColorOfTheFolder() = runTest {
        folderDao.insert(fakeFolder)

        val newColor = Color.RED
        val syncModified = System.currentTimeMillis()
        folderDao.updateFolderColor(fakeFolder.uuid, newColor, syncModified)

        val updatedFolder = folderDao.findByUuid(fakeFolder.uuid)
        assertNotNull(updatedFolder)
        assertEquals(newColor, updatedFolder?.color)
        assertEquals(syncModified, updatedFolder?.syncModified)
    }

    @Test
    fun updateFolderNameShouldUpdateTheNameOfTheFolder() = runTest {
        folderDao.insert(fakeFolder)

        val newName = "Updated Folder"
        val syncModified = System.currentTimeMillis()
        folderDao.updateFolderName(fakeFolder.uuid, newName, syncModified)

        val updatedFolder = folderDao.findByUuid(fakeFolder.uuid)
        assertNotNull(updatedFolder)
        assertEquals(newName, updatedFolder?.name)
        assertEquals(syncModified, updatedFolder?.syncModified)
    }

    @Test
    fun shouldReplaceOldFoldersWithNewOnes() = runTest {
        val initialFolders = listOf(
            fakeFolder.copy(uuid = "uuid1"),
            fakeFolder.copy(uuid = "uuid2"),
        )
        folderDao.insertAll(initialFolders)

        val newFolders = listOf(
            fakeFolder.copy(uuid = "uuid3"),
            fakeFolder.copy(uuid = "uuid4"),
        )

        folderDao.replaceAllFolders(newFolders, System.currentTimeMillis())

        val foundFolder1 = folderDao.findByUuid("uuid1")
        val foundFolder2 = folderDao.findByUuid("uuid2")
        assertEquals(true, foundFolder1!!.deleted)
        assertEquals(true, foundFolder2!!.deleted)

        val foundFolder3 = folderDao.findByUuid("uuid3")
        val foundFolder4 = folderDao.findByUuid("uuid4")
        assertNotNull(foundFolder3)
        assertNotNull(foundFolder4)
        assertEquals("uuid3", foundFolder3?.uuid)
        assertEquals("uuid4", foundFolder4?.uuid)
    }
}
