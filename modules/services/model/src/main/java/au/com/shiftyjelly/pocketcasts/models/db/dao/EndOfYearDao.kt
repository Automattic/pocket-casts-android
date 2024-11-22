package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastRatingGrouping
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast

@Dao
abstract class EndOfYearDao {
    @Query(
        """
        SELECT COUNT(*)
        FROM podcast_episodes AS episode
        WHERE
          episode.last_playback_interaction_date IS NOT NULL
          AND episode.last_playback_interaction_date >= :fromEpochMs
          AND episode.last_playback_interaction_date < :toEpochMs
        """,
    )
    abstract suspend fun getPlayedEpisodeCount(fromEpochMs: Long, toEpochMs: Long): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM podcast_episodes AS episode
        WHERE 
          episode.last_playback_interaction_date IS NOT NULL
          AND episode.last_playback_interaction_date >= :fromEpochMs
          AND episode.last_playback_interaction_date < :toEpochMs
          AND episode.playing_status IS 2
        """,
    )
    abstract suspend fun getCompletedEpisodeCount(fromEpochMs: Long, toEpochMs: Long): Int

    @Query(
        """
        SELECT DISTINCT podcast.uuid
        FROM podcasts AS podcast
          JOIN podcast_episodes AS episode
          ON podcast.uuid IS episode.podcast_id
        WHERE
          episode.last_playback_interaction_date IS NOT NULL
          AND episode.last_playback_interaction_date >= :fromEpochMs
          AND episode.last_playback_interaction_date < :toEpochMs
        """,
    )
    abstract suspend fun getPlayedPodcastIds(fromEpochMs: Long, toEpochMs: Long): List<String>

    @Query(
        """
        SELECT SUM(episode.played_up_to)
        FROM podcast_episodes AS episode
        WHERE
          episode.last_playback_interaction_date IS NOT NULL
          AND episode.last_playback_interaction_date >= :fromEpochMs
          AND episode.last_playback_interaction_date < :toEpochMs
        """,
    )
    abstract suspend fun getTotalPlaybackTime(fromEpochMs: Long, toEpochMs: Long): Double

    @Query(
        """
        SELECT
          podcast.uuid AS uuid,
          podcast.title AS title,
          podcast.author AS author,
          SUM(episode.played_up_to) AS playback_time_seconds,
          COUNT(episode.uuid) AS played_episode_count
        FROM podcast_episodes AS episode
          JOIN podcasts AS podcast
          ON podcast.uuid IS episode.podcast_id
        WHERE
          episode.last_playback_interaction_date IS NOT NULL
          AND episode.last_playback_interaction_date >= :fromEpochMs
          AND episode.last_playback_interaction_date < :toEpochMs
        GROUP BY podcast.uuid
        ORDER BY
          playback_time_seconds DESC,
          played_episode_count DESC
        LIMIT :limit
        """,
    )
    abstract suspend fun getTopPodcasts(fromEpochMs: Long, toEpochMs: Long, limit: Int): List<TopPodcast>

    @Query(
        """
        SELECT
          episode.uuid AS uuid,
          episode.title AS title,
          podcast.uuid AS podcast_uuid,
          podcast.title AS podcast_title,
          episode.duration AS duration_seconds,
          episode.image_url AS cover_url
        FROM podcast_episodes AS episode
          JOIN podcasts AS podcast
          ON podcast.uuid IS episode.podcast_id
        WHERE
          episode.last_playback_interaction_date IS NOT NULL
          AND episode.last_playback_interaction_date >= :fromEpochMs
          AND episode.last_playback_interaction_date < :toEpochMs
        ORDER BY episode.played_up_to DESC
        LIMIT 1
        """,
    )
    abstract suspend fun getLongestPlayedEpisode(fromEpochMs: Long, toEpochMs: Long): LongestEpisode?

    suspend fun getRatingStats(fromEpochMs: Long, toEpochMs: Long): RatingStats {
        val rating = getRatingGrouping(fromEpochMs, toEpochMs)
        return RatingStats(
            ones = rating.find { it.rating == 1 }?.count ?: 0,
            twos = rating.find { it.rating == 2 }?.count ?: 0,
            threes = rating.find { it.rating == 3 }?.count ?: 0,
            fours = rating.find { it.rating == 4 }?.count ?: 0,
            fives = rating.find { it.rating == 5 }?.count ?: 0,
        )
    }

    @Query(
        """
        SELECT
          rating AS rating,
          COUNT(rating) AS count
        FROM user_podcast_ratings
        WHERE
          modified_at >= :fromEpochMs
          AND modified_at < :toEpochMs
        GROUP BY rating
        """,
    )
    protected abstract suspend fun getRatingGrouping(fromEpochMs: Long, toEpochMs: Long): List<PodcastRatingGrouping>
}
