package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChat
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EpisodeChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertChat(chat: EpisodeChat)

    @Query("SELECT * FROM episode_chats WHERE episode_uuid = :episodeUuid")
    abstract suspend fun getChatByEpisode(episodeUuid: String): EpisodeChat?

    @Query("DELETE FROM episode_chats WHERE episode_uuid = :episodeUuid")
    abstract suspend fun deleteChat(episodeUuid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessage(message: EpisodeChatMessage)

    @Query("SELECT * FROM episode_chat_messages WHERE episode_uuid = :episodeUuid ORDER BY created_at ASC")
    abstract fun observeMessages(episodeUuid: String): Flow<List<EpisodeChatMessage>>

    @Query("SELECT * FROM episode_chat_messages WHERE episode_uuid = :episodeUuid ORDER BY created_at ASC")
    abstract suspend fun getMessages(episodeUuid: String): List<EpisodeChatMessage>

    @Query("DELETE FROM episode_chat_messages WHERE episode_uuid = :episodeUuid")
    abstract suspend fun deleteMessagesByEpisode(episodeUuid: String)
}
