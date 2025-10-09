package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastView

@Dao
abstract class ExternalDataDao {
    @Query(
        """
        SELECT 
          podcasts.uuid AS id, 
          podcasts.title AS title,
          podcasts.podcast_description AS description,
          podcasts.podcast_category AS podcast_category,
          (SELECT COUNT(*) FROM podcast_episodes WHERE podcasts.uuid IS podcast_episodes.podcast_id) AS episode_count,
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
            .mapValues { (listId, value) ->
                val listTitle = value[0].listTitle
                val podcasts = value.take(limitPerGroup).map { curatedPodcast ->
                    ExternalPodcastView(
                        id = curatedPodcast.podcastId,
                        title = curatedPodcast.podcastTitle,
                        description = curatedPodcast.podcastDescription,
                    )
                }
                ExternalPodcastList(listId, listTitle, podcasts)
            }
        return ExternalPodcastMap(listsMap, limitPerGroup)
    }

    @Query(
        """
        SELECT
          episode.uuid AS id,
          episode.podcast_id AS podcast_id,
          episode.title AS title,
          episode.duration * 1000 AS duration,
          episode.played_up_to * 1000 AS current_position,
          episode.season AS season_number,
          episode.number AS episode_number,
          episode.published_date AS release_timestamp,
          episode.last_playback_interaction_date AS last_used_timestamp,
          (episode.episode_status IS 4) AS is_downloaded,
          (episode.file_type LIKE 'video/%') AS is_video,
          podcast.title AS podcast_title
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
          episode.duration * 1000 AS duration,
          episode.played_up_to * 1000 AS current_position,
          episode.season AS season_number,
          episode.number AS episode_number,
          episode.published_date AS release_timestamp,
          episode.last_playback_interaction_date AS last_used_timestamp,
          (episode.episode_status IS 4) AS is_downloaded,
          (episode.file_type LIKE 'video/%') AS is_video,
          podcast.title AS podcast_title
        FROM
          podcast_episodes AS episode
          JOIN podcasts AS podcast ON podcast.uuid IS episode.podcast_id
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
}
