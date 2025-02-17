package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.SuggestedFoldersDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.utils.FakeFileGenerator
import com.squareup.moshi.Moshi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuggestedFoldersDaoTest {
    private lateinit var suggestedFolderDao: SuggestedFoldersDao
    private lateinit var testDatabase: AppDatabase

    @Before
    fun setupDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        suggestedFolderDao = testDatabase.suggestedFoldersDao()
    }

    @After
    fun closeDatabase() {
        testDatabase.clearAllTables()
        testDatabase.close()
    }

    @Test
    fun shouldStoreSuggestedFolder() = runTest {
        val fakeFolder = SuggestedFolder(
            name = "name",
            podcastUuid = FakeFileGenerator.uuid,
        )

        suggestedFolderDao.insert(fakeFolder)

        val foundFolder = suggestedFolderDao.findAllFolderPodcasts(fakeFolder.name)

        assertNotNull(foundFolder)
        assertEquals(fakeFolder.name, foundFolder[0].name)
    }

    @Test
    fun shouldRetrieveAllFolderPodcasts() = runTest {
        val item1 = SuggestedFolder(
            name = "folder1",
            podcastUuid = "1234",
        )

        val item2 = SuggestedFolder(
            name = "folder1",
            podcastUuid = "5678",
        )

        val item3 = SuggestedFolder(
            name = "folder2",
            podcastUuid = "9876",
        )

        suggestedFolderDao.insert(item1)
        suggestedFolderDao.insert(item2)
        suggestedFolderDao.insert(item3)

        val folder = suggestedFolderDao.findAllFolderPodcasts("folder1")
        assertEquals(2, folder.size)
    }
}
