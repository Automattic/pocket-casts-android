package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import au.com.shiftyjelly.pocketcasts.utils.extensions.md5
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.moshi.Moshi
import java.util.Date
import java.util.UUID
import junit.framework.TestCase.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SuggestedFoldersManagerTest {
    private val clock = MutableClock()
    private val uuidProvider = object : UUIDProvider {
        val uuids = List(100) { UUID.randomUUID() }
        private var counter = 0

        override fun generateUUID(): UUID {
            val uuid = uuids[counter % uuids.size]
            counter++
            return uuid
        }
    }
    private val cacheService = mock<PodcastCacheServiceManager>()
    private lateinit var appDatabase: AppDatabase
    private lateinit var settings: Settings

    private lateinit var manager: SuggestedFoldersManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val moshi = Moshi.Builder().build()
        val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(moshi))
            .build()
        settings = SettingsImpl(
            sharedPreferences = preferences,
            privatePreferences = preferences,
            context = context,
            firebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
            moshi = moshi,
        )

        manager = SuggestedFoldersManager(
            database = appDatabase,
            cahceServiceManager = cacheService,
            settings = settings,
            clock = clock,
            uuidProvider = uuidProvider,
        )
    }

    @Test
    fun insertSuggestedFolders() = runBlocking {
        appDatabase.podcastDao().insertBlocking(Podcast(uuid = "uuid", isSubscribed = true))
        val folders = listOf(
            SuggestedFolder("Folder1", "uuid"),
        )

        whenever(cacheService.suggestedFolders(any())).thenReturn(folders)
        manager.refreshSuggestedFolders()

        assertEquals(folders, appDatabase.suggestedFoldersDao().findAll().first())
    }

    @Test
    fun doNotRequestSuggestedFoldersWhenNoPodcastChanges() = runBlocking {
        appDatabase.podcastDao().insertBlocking(Podcast(uuid = "uuid", isSubscribed = true))
        settings.suggestedFoldersFollowedHash.set(listOf("uuid").md5()!!, updateModifiedAt = false)
        whenever(cacheService.suggestedFolders(any())).thenThrow(AssertionError("Shouldn't happen"))

        manager.refreshSuggestedFolders()

        assertEquals(emptyList<SuggestedFolder>(), appDatabase.suggestedFoldersDao().findAll().first())
    }

    @Test
    fun doNotInsertSuggestedFoldersWhenServiceFails() = runBlocking {
        appDatabase.podcastDao().insertBlocking(Podcast(uuid = "uuid", isSubscribed = true))
        whenever(cacheService.suggestedFolders(any())).thenThrow(RuntimeException("Test Exception"))

        manager.refreshSuggestedFolders()

        assertEquals(emptyList<SuggestedFolder>(), appDatabase.suggestedFoldersDao().findAll().first())
    }

    @Test
    fun useSuggestedFolders() = runBlocking {
        settings.podcastsSortType.set(PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST, updateModifiedAt = true)
        with(appDatabase.podcastDao()) {
            insertBlocking(Podcast(uuid = "uuid-1", isSubscribed = true))
            insertBlocking(Podcast(uuid = "uuid-2", isSubscribed = true))
            insertBlocking(Podcast(uuid = "uuid-3", isSubscribed = true, rawFolderUuid = "pre-existing-folder"))
        }
        appDatabase.folderDao().insert(
            Folder(
                uuid = "pre-existing-folder",
                name = "name",
                color = 0,
                addedDate = Date(),
                sortPosition = 0,
                podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
                deleted = false,
                syncModified = 0,
            ),
        )
        val suggestedFolders = listOf(
            SuggestedFolder("Folder 1", "uuid-1"),
            SuggestedFolder("Folder 1", "uuid-2"),
            SuggestedFolder("Folder 2", "uuid-3"),
        )
        appDatabase.suggestedFoldersDao().insertAll(suggestedFolders)

        clock += 100.milliseconds
        manager.useSuggestedFolders(suggestedFolders)

        assertEquals(emptyList<SuggestedFolder>(), appDatabase.suggestedFoldersDao().findAll().first())
        assertEquals(
            listOf(
                Podcast(
                    uuid = "uuid-1",
                    isSubscribed = true,
                    rawFolderUuid = uuidProvider.uuids[0].toString(),
                    syncStatus = 0,
                ),
                Podcast(
                    uuid = "uuid-2",
                    isSubscribed = true,
                    rawFolderUuid = uuidProvider.uuids[0].toString(),
                    syncStatus = 0,
                ),
                Podcast(
                    uuid = "uuid-3",
                    isSubscribed = true,
                    rawFolderUuid = uuidProvider.uuids[1].toString(),
                    syncStatus = 0,
                ),
            ),
            appDatabase.podcastDao().findSubscribedBlocking(),
        )
        assertEquals(
            listOf(
                Folder(
                    uuid = uuidProvider.uuids[0].toString(),
                    name = "Folder 1",
                    color = 0,
                    addedDate = Date(100),
                    sortPosition = 0,
                    podcastsSortType = PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST,
                    deleted = false,
                    syncModified = 100,
                ),
                Folder(
                    uuid = uuidProvider.uuids[1].toString(),
                    name = "Folder 2",
                    color = 1,
                    addedDate = Date(100),
                    sortPosition = 0,
                    podcastsSortType = PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST,
                    deleted = false,
                    syncModified = 100,
                ),
            ),
            appDatabase.folderDao().findFolders(),
        )
        assertEquals(PodcastsSortType.NAME_A_TO_Z, settings.podcastsSortType.value)
    }
}
