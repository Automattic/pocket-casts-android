package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherQueueEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.toUpNextEpisode
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UpNextDao {

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC")
    abstract fun allLiveData(): LiveData<List<UpNextEpisode>>

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC")
    abstract fun allRx(): Single<List<UpNextEpisode>>

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC")
    abstract fun observeAll(): Flowable<List<UpNextEpisode>>

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC")
    abstract fun all(): List<UpNextEpisode>

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC LIMIT 50")
    abstract fun allLimit(): List<UpNextEpisode>

    @Query("SELECT * FROM up_next_episodes WHERE episodeUuid = :uuid")
    abstract fun findByEpisodeUuid(uuid: String): UpNextEpisode?

    @Query("SELECT * FROM up_next_episodes WHERE episodeUuid = :uuid")
    abstract fun findByEpisodeUuidRx(uuid: String): Maybe<UpNextEpisode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(episode: UpNextEpisode): Long

    /**
     * Add an episode to the Up Next.
     * position 0: Top 1: Next -1: Last
     */
    @Transaction
    open fun insertAt(upNextEpisode: UpNextEpisode, position: Int, replaceOneEpisode: Boolean = false) {
        // remove the episode before we add it to the top
        deleteByUuid(upNextEpisode.episodeUuid)
        // find all the existing Up Next episodes
        val upNextEpisodes: List<UpNextEpisode> = all()
        // update the existing episode positions
        val newPosition = updatePositions(upNextEpisodes, position, replaceOneEpisode)
        upNextEpisode.position = newPosition
        // add the episode
        insert(upNextEpisode)
    }

    private fun updatePositions(episodes: List<UpNextEpisode>, position: Int, replaceOneEpisode: Boolean): Int {
        val addLast = position == -1
        val newPosition = if (addLast) episodes.size else position

        if (episodes.isEmpty()) {
            // do nothing
        } else if (replaceOneEpisode && episodes.size == 1) {
            deleteAll()
        } else {
            changeEpisodesPositions(episodes, position)
        }
        return newPosition
    }

    private fun changeEpisodesPositions(upNextEpisodes: List<UpNextEpisode>, insertPosition: Int) {
        // there's more than one thing in our up next queue, so move the requested episode to the top, and the currently playing one down one
        upNextEpisodes.forEachIndexed { index, upNextEpisode ->
            val position = if (insertPosition == 1 && index == 0) {
                // play next keep the top episode at position 0
                0
            } else {
                index + 1
            }
            upNextEpisode.id?.let { id -> updatePosition(id = id, position = position) }
        }
    }

    @Transaction
    open fun insertAll(episodes: List<UpNextEpisode>) {
        episodes.forEach { insert(it) }
    }

    @Update
    abstract fun update(episode: UpNextEpisode)

    @Query("UPDATE up_next_episodes SET position = :position WHERE _id = :id")
    abstract fun updatePosition(id: Long, position: Int)

    @Delete
    abstract fun delete(episode: UpNextEpisode)

    @Query("DELETE FROM up_next_episodes")
    abstract fun deleteAll()

    @Query("DELETE FROM up_next_episodes WHERE episodeUuid NOT IN (:uuids)")
    abstract fun deleteWhereNotUuids(uuids: List<String>)

    @Query("DELETE FROM up_next_episodes WHERE episodeUuid IN (:uuids)")
    abstract fun deleteWhereUuids(uuids: List<String>)

    @Transaction
    open fun deleteAllNotCurrent() {
        val currentEpisodeUuid = findCurrentUpNextEpisode()?.episodeUuid ?: return
        deleteAllNotUuid(currentEpisodeUuid)
    }

    @Query("DELETE FROM up_next_episodes WHERE episodeUuid != :uuid")
    abstract fun deleteAllNotUuid(uuid: String)

    @Query("DELETE FROM up_next_episodes WHERE episodeUuid = :uuid")
    abstract fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC LIMIT 1")
    abstract fun findCurrentUpNextEpisode(): UpNextEpisode?

    @Query("SELECT podcast_episodes.* FROM up_next_episodes JOIN podcast_episodes ON podcast_episodes.uuid = up_next_episodes.episodeUuid ORDER BY up_next_episodes.position ASC")
    abstract fun findEpisodes(): List<PodcastEpisode>

    @Query("SELECT user_episodes.* FROM up_next_episodes JOIN user_episodes ON user_episodes.uuid = up_next_episodes.episodeUuid ORDER BY up_next_episodes.position ASC")
    abstract fun findUserEpisodes(): List<UserEpisode>

    @Query("SELECT episodeUuid FROM up_next_episodes ORDER BY up_next_episodes.position ASC")
    abstract fun findEpisodeUuids(): List<String>

    @Query("SELECT COUNT(*) FROM up_next_episodes")
    abstract fun count(): Int

    @Query("SELECT COUNT(*) FROM up_next_episodes WHERE episodeUuid = :episodeUuid")
    abstract fun countByEpisode(episodeUuid: String): Int

    open fun containsEpisode(episodeUuid: String): Boolean {
        return countByEpisode(episodeUuid) > 0
    }

    @Transaction
    open fun findAllEpisodesSorted(): List<BaseEpisode> {
        val episodes = findEpisodes().associateBy { it.uuid }
        val userEpisodes = findUserEpisodes().associateBy { it.uuid }
        val orders = findEpisodeUuids()

        return orders.mapNotNull {
            episodes[it] as BaseEpisode? ?: userEpisodes[it] as BaseEpisode?
        }
    }

    @Transaction
    open fun saveAll(episodes: List<BaseEpisode>) {
        val newUpNextEpisodes = episodes.map(BaseEpisode::toUpNextEpisode)
        val databaseUpNextEpisodes = all()
        val uuidToId = databaseUpNextEpisodes.associateBy({ it.episodeUuid }, { it.id })

        for (i in newUpNextEpisodes.indices) {
            val episode = newUpNextEpisodes[i]
            // match the episode to an Up Next episode database id or insert it
            val id = uuidToId[episode.episodeUuid]
            if (id == null) {
                episode.position = i
                insert(episode)
            } else {
                updatePosition(id = id, position = i)
            }
        }
        // delete old Up Next episodes if they no longer exist
        val databaseUuids = databaseUpNextEpisodes.map(UpNextEpisode::episodeUuid)
        val newUuids = episodes.map(BaseEpisode::uuid)
        databaseUuids.minus(newUuids).forEach(this::deleteByUuid)
    }

    @Transaction
    open suspend fun getUpNextBaseEpisodes(limit: Int): List<BaseEpisode> {
        val upNextEpisodes = getUpNextEpisodes(limit)
        val idToPosition = upNextEpisodes.associate { it.episodeUuid to it.position }
        val podcastEpisodes = findPodcastEpisodes(idToPosition.keys)
        val userEpisodes = if (podcastEpisodes.size != upNextEpisodes.size) findUserEpisodes(idToPosition.keys) else emptyList()
        return (podcastEpisodes + userEpisodes).sortedBy { idToPosition[it.uuid] }
    }

    @Query("SELECT * FROM up_next_episodes ORDER BY position ASC LIMIT :limit")
    protected abstract suspend fun getUpNextEpisodes(limit: Int): List<UpNextEpisode>

    @Query("SELECT * FROM podcast_episodes WHERE uuid IN (:ids)")
    protected abstract suspend fun findPodcastEpisodes(ids: Collection<String>): List<PodcastEpisode>

    @Query("SELECT * FROM user_episodes WHERE uuid IN (:ids)")
    protected abstract suspend fun findUserEpisodes(ids: Collection<String>): List<UserEpisode>

    @Query(
        """
        SELECT
          episode.*
        FROM 
          up_next_episodes AS up_next 
          LEFT JOIN (
            SELECT
              -- common properties
              TRUE AS is_podcast_episode, 
              episode.uuid AS id, 
              episode.title AS title, 
              episode.duration AS duration, 
              episode.played_up_to AS current_position, 
              episode.published_date AS release_timestamp, 
              -- podcast episode properties
              episode.podcast_id AS podcast_id, 
              episode.season AS season_number, 
              episode.number AS episode_number, 
              episode.last_playback_interaction_date AS last_used_timestamp,
              -- user episode properties
              NULL AS artwork_url, 
              NULL AS tint_color_index
            FROM 
              podcast_episodes AS episode 
            UNION ALL 
            SELECT
              -- common properties
              FALSE AS is_podcast_episode, 
              episode.uuid AS id, 
              episode.title AS title, 
              episode.duration AS duration, 
              episode.played_up_to AS current_position, 
              episode.published_date AS release_timestamp,
              -- podcast episode properties
              NULL AS podcast_id, 
              NULL AS season_number, 
              NULL AS episode_number, 
              NULL AS last_used_timestamp,
              -- user episode properties
              episode.artwork_url AS artwork_url, 
              episode.tint_color_index AS tint_color_index
            FROM 
              user_episodes AS episode
          ) AS episode ON up_next.episodeUuid IS episode.id 
        WHERE
          episode.id IS NOT NULL
        ORDER BY 
          up_next.position ASC 
        LIMIT 
          :limit
        """,
    )
    abstract fun getNovaLauncherQueueEpisodes(limit: Int): Flow<List<NovaLauncherQueueEpisode>>
}
