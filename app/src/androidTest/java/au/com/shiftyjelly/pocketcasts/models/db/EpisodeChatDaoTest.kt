package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeChatDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import com.squareup.moshi.Moshi
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EpisodeChatDaoTest {
    lateinit var episodeChatDao: EpisodeChatDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        episodeChatDao = testDb.episodeChatDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun testInsertAndGetChat() = runTest {
        val chat = EpisodeChat(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            createdAt = Date(100),
        )

        episodeChatDao.insertChat(chat)

        assertEquals(chat, episodeChatDao.getChatByEpisode(EPISODE_UUID))
        assertNull(episodeChatDao.getChatByEpisode("missing-episode-uuid"))
    }

    @Test
    fun testReplaceChat() = runTest {
        episodeChatDao.insertChat(EpisodeChat(episodeUuid = EPISODE_UUID, podcastUuid = "old-podcast-uuid", createdAt = Date(100)))
        val replacement = EpisodeChat(
            episodeUuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            createdAt = Date(200),
        )

        episodeChatDao.insertChat(replacement)

        assertEquals(replacement, episodeChatDao.getChatByEpisode(EPISODE_UUID))
    }

    @Test
    fun testInsertAndGetMessagesOrderedByCreatedAt() = runTest {
        val later = createMessage(uuid = "later-message-uuid", text = "Later", createdAt = Date(200))
        val earlier = createMessage(uuid = "earlier-message-uuid", text = "Earlier", createdAt = Date(100))

        episodeChatDao.insertMessage(later)
        episodeChatDao.insertMessage(earlier)

        assertEquals(listOf(earlier, later), episodeChatDao.getMessages(EPISODE_UUID))
        assertEquals(listOf(earlier, later), episodeChatDao.observeMessages(EPISODE_UUID).first())
    }

    @Test
    fun testReplaceMessage() = runTest {
        episodeChatDao.insertMessage(createMessage(uuid = "message-uuid", text = "Old text", createdAt = Date(100)))
        val replacement = createMessage(uuid = "message-uuid", text = "New text", createdAt = Date(200))

        episodeChatDao.insertMessage(replacement)

        assertEquals(listOf(replacement), episodeChatDao.getMessages(EPISODE_UUID))
    }

    @Test
    fun testDeleteMessagesByEpisode() = runTest {
        val message = createMessage(uuid = "message-uuid", episodeUuid = EPISODE_UUID, text = "Message")
        val otherMessage = createMessage(uuid = "other-message-uuid", episodeUuid = "other-episode-uuid", text = "Other message")
        episodeChatDao.insertMessage(message)
        episodeChatDao.insertMessage(otherMessage)

        episodeChatDao.deleteMessagesByEpisode(EPISODE_UUID)

        assertEquals(emptyList<EpisodeChatMessage>(), episodeChatDao.getMessages(EPISODE_UUID))
        assertEquals(listOf(otherMessage), episodeChatDao.getMessages("other-episode-uuid"))
    }

    private fun createMessage(
        uuid: String,
        episodeUuid: String = EPISODE_UUID,
        text: String,
        createdAt: Date = Date(100),
    ) = EpisodeChatMessage(
        uuid = uuid,
        episodeUuid = episodeUuid,
        text = text,
        role = "assistant",
        createdAt = createdAt,
    )

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
    }
}
