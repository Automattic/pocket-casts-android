package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EpisodeChatDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertChatIfAbsent(chat: EpisodeChat)

    @Query("SELECT * FROM episode_chats WHERE episode_uuid = :episodeUuid")
    abstract suspend fun getChatByEpisode(episodeUuid: String): EpisodeChat?

    @Query("DELETE FROM episode_chats WHERE episode_uuid = :episodeUuid")
    abstract suspend fun deleteChat(episodeUuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessage(message: EpisodeChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessages(messages: List<EpisodeChatMessage>)

    @Query("SELECT * FROM episode_chat_messages WHERE episode_uuid = :episodeUuid ORDER BY created_at ASC")
    abstract fun observeMessages(episodeUuid: String): Flow<List<EpisodeChatMessage>>

    @Query("SELECT * FROM episode_chat_messages WHERE episode_uuid = :episodeUuid ORDER BY created_at ASC")
    abstract suspend fun getMessages(episodeUuid: String): List<EpisodeChatMessage>

    @Query("DELETE FROM episode_chat_messages WHERE uuid = :uuid")
    abstract suspend fun deleteMessage(uuid: String)

    @Query("DELETE FROM episode_chat_messages WHERE episode_uuid = :episodeUuid")
    abstract suspend fun deleteMessagesByEpisode(episodeUuid: String)

    @Transaction
    open suspend fun takeMessages(episodeUuid: String): List<EpisodeChatMessage> {
        val messages = getMessages(episodeUuid)
        deleteMessagesByEpisode(episodeUuid)
        return messages
    }

    @Query(
        """
        SELECT
          episode_chats.episode_uuid AS episode_uuid,
          podcast_episodes.title AS episode_title,
          podcast_episodes.podcast_id AS podcast_uuid,
          IFNULL(podcasts.title, '') AS podcast_title,
          podcast_episodes.duration AS episode_duration,
          MAX(episode_chat_messages.created_at) AS last_message_at
        FROM episode_chats
        JOIN episode_chat_messages ON episode_chat_messages.episode_uuid = episode_chats.episode_uuid
        JOIN podcast_episodes ON podcast_episodes.uuid = episode_chats.episode_uuid
        LEFT JOIN podcasts ON podcasts.uuid = podcast_episodes.podcast_id
        GROUP BY episode_chats.episode_uuid
        ORDER BY last_message_at DESC
        """,
    )
    abstract fun observeChatHistory(): Flow<List<EpisodeChatHistoryItem>>

    @Query("UPDATE episode_chat_messages SET rating = :rating WHERE uuid = :uuid")
    abstract suspend fun updateRating(uuid: String, rating: String?)
}
