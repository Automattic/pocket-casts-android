package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.squareup.moshi.Moshi
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeChatDaoTest {
    private lateinit var testDb: AppDatabase
    private lateinit var episodeChatDao: EpisodeChatDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        episodeChatDao = testDb.episodeChatDao()
        testDb.episodeDao().insertBlocking(PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date()))
        testDb.episodeDao().insertBlocking(PodcastEpisode(uuid = OTHER_EPISODE_UUID, publishedDate = Date()))
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun chatWithMessagesIsFound() = runTest {
        episodeChatDao.insertChatIfAbsent(EpisodeChat(episodeUuid = EPISODE_UUID))
        episodeChatDao.insertMessage(EpisodeChatMessage(uuid = "message", episodeUuid = EPISODE_UUID, text = "Question", role = "user"))

        assertTrue(episodeChatDao.hasChatWithMessages(EPISODE_UUID))
    }

    @Test
    fun emptyChatIsNotFound() = runTest {
        episodeChatDao.insertChatIfAbsent(EpisodeChat(episodeUuid = EPISODE_UUID))

        assertFalse(episodeChatDao.hasChatWithMessages(EPISODE_UUID))
    }

    @Test
    fun missingChatIsNotFound() = runTest {
        assertFalse(episodeChatDao.hasChatWithMessages(EPISODE_UUID))
    }

    @Test
    fun anotherEpisodeChatIsNotFound() = runTest {
        episodeChatDao.insertChatIfAbsent(EpisodeChat(episodeUuid = OTHER_EPISODE_UUID))
        episodeChatDao.insertMessage(EpisodeChatMessage(uuid = "message", episodeUuid = OTHER_EPISODE_UUID, text = "Question", role = "user"))

        assertFalse(episodeChatDao.hasChatWithMessages(EPISODE_UUID))
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val OTHER_EPISODE_UUID = "other-episode-uuid"
    }
}
