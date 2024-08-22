package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastOrUserEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastView
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class ExternalDataDao {
    @Query(
        """
        SELECT 
          podcasts.uuid AS id, 
          podcasts.title AS title, 
          podcasts.podcast_category AS podcast_category,
          (SELECT MIN(podcast_episodes.published_date) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS initial_release_timestamp, 
          (SELECT MAX(podcast_episodes.published_date) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS latest_release_timestamp,
          (SELECT MAX(podcast_episodes.last_playback_interaction_date) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS last_used_timestamp
        FROM 
          podcasts 
        WHERE 
          podcasts.subscribed IS NOT 0
        ORDER BY
          -- Order by oldest to newest date added
          CASE WHEN :sortOrder IS 0 THEN IFNULL(podcasts.added_date, 9223372036854775807) END ASC,
          -- Order by A-Z podcast title
          CASE WHEN :sortOrder IS 1 THEN (CASE
            WHEN UPPER(podcasts.title) LIKE 'THE %' THEN SUBSTR(UPPER(podcasts.title), 5)
            WHEN UPPER(podcasts.title) LIKE 'A %' THEN SUBSTR(UPPER(podcasts.title), 3)
            WHEN UPPER(podcasts.title) LIKE 'AN %' THEN SUBSTR(UPPER(podcasts.title), 4)
            ELSE UPPER(podcasts.title)
          END) END ASC,
          -- Order by newest to oldest episode
          CASE WHEN :sortOrder IS 2 THEN (SELECT IFNULL(MAX(podcast_episodes.published_date), 0) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) END DESC,
          -- Order by drag and drop position
          CASE WHEN :sortOrder IS 3 THEN IFNULL(podcasts.sort_order, 9223372036854775807) END ASC
        LIMIT
          :limit
        """,
    )
    abstract suspend fun getSubscribedPodcasts(
        sortOrder: PodcastsSortType,
        limit: Int,
    ): List<ExternalPodcast>

    @Query(
        """
        SELECT 
          podcasts.uuid AS id, 
          podcasts.title AS title,
          podcasts.podcast_category AS podcast_category,
          (SELECT MIN(podcast_episodes.published_date) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS initial_release_timestamp, 
          (SELECT MAX(podcast_episodes.published_date) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS latest_release_timestamp,
          (SELECT MAX(podcast_episodes.last_playback_interaction_date) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS last_used_timestamp
        FROM 
          podcasts
        WHERE
        -- Select only episodes that were used at most 2 months ago
          last_used_timestamp >= (:currentTime - 5184000000)
        ORDER BY
          last_used_timestamp DESC
        LIMIT
          :limit
        """,
    )
    abstract suspend fun getRecentlyPlayedPodcasts(
        limit: Int,
        currentTime: Long = System.currentTimeMillis(),
    ): List<ExternalPodcast>

    @Query(
        """
        SELECT 
          curated_podcast.*
        FROM 
          curated_podcasts as curated_podcast 
          LEFT JOIN podcasts AS podcast ON podcast.uuid = curated_podcast.podcast_id 
        WHERE 
          IFNULL(podcast.subscribed, 0) IS 0
          OR curated_podcast.list_id IS '${CuratedPodcast.FEATURED_LIST_ID}'
        """,
    )
    protected abstract suspend fun getCuratedPodcasts(): List<CuratedPodcast>

    final suspend fun getCuratedPodcastGroups(limitPerGroup: Int): ExternalPodcastMap {
        val listsMap = getCuratedPodcasts()
            .groupBy { it.listId }
            .mapValues { (_, value) ->
                val listTitle = value[0].listTitle
                val podcasts = value.take(limitPerGroup).map { curatedPodcast ->
                    ExternalPodcastView(
                        id = curatedPodcast.podcastId,
                        title = curatedPodcast.podcastTitle,
                        description = curatedPodcast.podcastDescription,
                    )
                }
                ExternalPodcastList(listTitle, podcasts)
            }
        return ExternalPodcastMap(listsMap)
    }

    @Query(
        """
        SELECT
          episode.uuid AS id,
          episode.podcast_id AS podcast_id,
          episode.title AS title,
          episode.duration AS duration,
          episode.played_up_to AS current_position,
          episode.season AS season_number,
          episode.number AS episode_number,
          episode.published_date AS release_timestamp,
          episode.last_playback_interaction_date AS last_used_timestamp
        FROM
          podcast_episodes AS episode
          JOIN podcasts AS podcast ON podcast.uuid IS episode.podcast_id
        WHERE
          episode.archived IS 0
          AND podcast.subscribed IS 1
          -- Check that the episode is not played
          AND episode.playing_status IS 0
          -- Select only episodes that were released at most 2 weeks ago
          AND episode.published_date >= (:currentTime - 1209600000)
        ORDER BY
          episode.published_date DESC
        LIMIT
          :limit
        """,
    )
    abstract suspend fun getNewEpisodes(
        limit: Int,
        currentTime: Long = System.currentTimeMillis(),
    ): List<ExternalEpisode.Podcast>

    @Query(
        """
        SELECT
          episode.uuid AS id,
          episode.podcast_id AS podcast_id,
          episode.title AS title,
          episode.duration AS duration,
          episode.played_up_to AS current_position,
          episode.season AS season_number,
          episode.number AS episode_number,
          episode.published_date AS release_timestamp,
          episode.last_playback_interaction_date AS last_used_timestamp
        FROM
          podcast_episodes AS episode
        WHERE
          episode.archived IS 0
          -- Check that the episode is in progress
          AND episode.playing_status IS 1
          -- Select only episodes that were released at most 2 weeks ago
          AND episode.published_date >= (:currentTime - 1209600000)
        ORDER BY
          episode.last_playback_interaction_date DESC
        LIMIT
          :limit
        """,
    )
    abstract suspend fun getInProgressEpisodes(
        limit: Int,
        currentTime: Long = System.currentTimeMillis(),
    ): List<ExternalEpisode.Podcast>

    @Query(
        """
        SELECT
          episode.*
        FROM 
          up_next_episodes AS up_next 
          LEFT JOIN (
            SELECT
              -- common properties
              1 AS is_podcast_episode, 
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
              0 AS is_podcast_episode, 
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
    protected abstract fun _observerUpNexteQueue(limit: Int): Flow<List<ExternalPodcastOrUserEpisode>>

    final fun observeUpNextQueue(limit: Int) = _observerUpNexteQueue(limit).map { queue ->
        queue.mapNotNull { it.toExternalEpisode() }
    }
}
