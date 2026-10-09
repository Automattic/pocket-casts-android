package au.com.shiftyjelly.pocketcasts.repositories.chat

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class LoggingChatFeedbackManagerTest {
    private val tree = RecordingTree()
    private val manager = LoggingChatFeedbackManager()

    @Before
    fun setUp() {
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uproot(tree)
    }

    @Test
    fun `logs the feedback summary under the episode chat tag`() = runTest {
        manager.submit(feedback(reason = ChatFeedback.Reason.WrongFacts, details = "Wrong year"))

        val log = tree.logs.single()
        assertEquals("EpisodeChat", log.tag)
        assertTrue(log.message.contains("episode=$EPISODE_UUID"))
        assertTrue(log.message.contains("reason=WrongFacts"))
        assertTrue(log.message.contains("hasDetails=true"))
        assertTrue(log.message.contains("messages=2"))
    }

    @Test
    fun `reports no details when they are blank`() = runTest {
        manager.submit(feedback(details = "   "))

        assertTrue(tree.logs.single().message.contains("hasDetails=false"))
    }

    @Test
    fun `does not log the details or the conversation`() = runTest {
        manager.submit(feedback(details = "Wrong year"))

        val message = tree.logs.single().message
        assertFalse(message.contains("Wrong year"))
        assertFalse(message.contains(QUESTION))
        assertFalse(message.contains(ANSWER))
    }

    private fun feedback(
        reason: ChatFeedback.Reason = ChatFeedback.Reason.Other,
        details: String = "",
    ) = ChatFeedback(
        episodeUuid = EPISODE_UUID,
        podcastUuid = PODCAST_UUID,
        reason = reason,
        details = details,
        conversation = listOf(ChatMessage.User(QUESTION), ChatMessage.Assistant(ANSWER)),
    )

    private class RecordingTree : Timber.Tree() {
        val logs = mutableListOf<Log>()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logs += Log(tag, message)
        }
    }

    private data class Log(val tag: String?, val message: String)

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val PODCAST_UUID = "podcast-uuid"
        const val QUESTION = "When was this recorded?"
        const val ANSWER = "In 2019."
    }
}
