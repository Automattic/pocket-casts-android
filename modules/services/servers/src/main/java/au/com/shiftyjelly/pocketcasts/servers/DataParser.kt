package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Share
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.servers.discover.PodcastSearch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DataParser {

    private val DATE_PARSER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    init {
        DATE_PARSER.timeZone = TimeZone.getTimeZone("GMT")
    }

    fun parseExportFeedUrls(data: String?): Map<String, String>? {
        data ?: return null
        try {
            val jsonObject = JSONObject(data)
            val items = HashMap<String, String>()
            val keyItr = jsonObject.keys()
            while (keyItr.hasNext()) {
                val key = keyItr.next() as String
                val value = getString(jsonObject, key)
                if (value != null && value.isNotBlank()) {
                    items[key] = value
                }
            }
            return items
        } catch (e: Exception) {
            Timber.e(e, "Problems parsing podcast headers.")
            return null // TODO
        }
    }

    fun parsePodcastSearch(data: String?, searchTerm: String): PodcastSearch {
        val search = PodcastSearch(searchTerm = searchTerm)

        data ?: return search
        try {
            val result = JSONObject(data)
            val podcastJson = result.optJSONObject("podcast")
            val searchResultsJson = result.optJSONArray("search_results")

            if (podcastJson != null) {
                val podcast = parsePodcastFromJsonObject(podcastJson)
                if (podcast != null) {
                    search.searchResults.add(
                        Podcast(
                            uuid = podcast.uuid,
                            title = podcast.title,
                            author = podcast.author
                        )
                    )
                }
            }
            search.isUrl = result.optBoolean("is_url", false)
            if (searchResultsJson != null) {
                for (i in 0 until searchResultsJson.length()) {
                    val json = searchResultsJson.getJSONObject(i) ?: continue
                    val podcast = Podcast()
                    podcast.uuid = getString(json, "uuid") ?: continue
                    podcast.title = getString(json, "title") ?: ""
                    podcast.author = getString(json, "author") ?: ""
                    search.searchResults.add(podcast)
                }
            }

            return search
        } catch (e: Exception) {
            Timber.e(e, "Problems parsing podcast search.")
            return search
        }
    }

    fun parseShareItem(data: String?): Share? {
        data ?: return null
        try {
            val result = JSONObject(data)
            val podcastJson = result.optJSONObject("podcast") ?: return null
            val podcast = parsePodcastFromJsonObject(podcastJson) ?: return null
            val episode = result.optJSONObject("shared_episode")?.let { parseEpisodeFromJson(it, podcast.uuid) }
            val timeInSeconds = result.optInt("time")
            val message = getString(result, "message")
            return Share(
                podcast = podcast,
                episode = episode,
                timeInSeconds = timeInSeconds,
                message = message
            )
        } catch (e: Exception) {
            Timber.e(e, "Problems parsing share item.")
            return null
        }
    }

    fun parseRefreshPodcasts(data: String?): RefreshResponse? {
        data ?: return null
        try {
            val refreshMap = JSONObject(data).getJSONObject("podcast_updates")

            var podcastUuid: String
            var updatedEpisodes: JSONArray
            val response = RefreshResponse()
            val it = refreshMap.keys()
            while (it.hasNext()) {
                podcastUuid = it.next()
                updatedEpisodes = refreshMap.getJSONArray(podcastUuid)
                val newEpisodes = ArrayList<Episode>()
                for (i in 0 until updatedEpisodes.length()) {
                    parseEpisodeFromJson(updatedEpisodes.getJSONObject(i), podcastUuid)?.let { episode ->
                        newEpisodes.add(episode)
                    }
                }
                if (newEpisodes.size > 0) {
                    response.addUpdate(podcastUuid, newEpisodes)
                }
            }

            return response
        } catch (e: Exception) {
            Timber.e(e, "Problems refreshing podcasts.")
            return null // TODO
        }
    }

    private fun parseEpisodeFromJson(jsonEpisode: JSONObject, podcastUuid: String?): Episode? {
        val uuid = getString(jsonEpisode, "uuid")
        val publishedAt = getDate(jsonEpisode, "published_at")
        val podcastUuidOrJson = podcastUuid ?: getString(jsonEpisode, "podcast_uuid")
        if (uuid == null || publishedAt == null || podcastUuidOrJson == null) {
            return null
        }
        return Episode(
            episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED,
            playingStatus = EpisodePlayingStatus.NOT_PLAYED,
            title = getString(jsonEpisode, "title") ?: "",
            uuid = uuid,
            downloadUrl = getString(jsonEpisode, "url"),
            sizeInBytes = getLong(jsonEpisode, "size_in_bytes"),
            duration = getDouble(jsonEpisode, "duration_in_secs"),
            episodeDescription = getString(jsonEpisode, "description") ?: "",
            fileType = getString(jsonEpisode, "file_type"),
            publishedDate = publishedAt,
            podcastUuid = podcastUuidOrJson,
            addedDate = Date()
        )
    }

    fun getString(jsonObject: JSONObject, key: String): String? {
        return try {
            if (jsonObject.isNull(key)) null else jsonObject.getString(key)
        } catch (e: Exception) {
            null
        }
    }

    private fun getLong(jsonObject: JSONObject, key: String): Long {
        return try {
            jsonObject.getLong(key)
        } catch (e: Exception) {
            0
        }
    }

    private fun getDouble(jsonObject: JSONObject, key: String): Double {
        return try {
            jsonObject.getDouble(key)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getDate(jsonObject: JSONObject, key: String): Date? {
        try {
            val date = (if (jsonObject.isNull(key)) null else jsonObject.getString(key)) ?: return null

            return DATE_PARSER.parse(date)
        } catch (e: Exception) {
            return null
        }
    }

    fun parseServerResponse(data: String?): ServerResponse {
        data ?: return ServerResponse(success = false)
        try {
            val allData = JSONObject(data)
            val message = getString(allData, "message")
            val messageId = getString(allData, "messageId")
            val status = allData.getString("status")
            if (status == "ok") {
                return ServerResponse(
                    success = true,
                    data = getString(allData, "result"),
                    token = getString(allData, "token")
                )
            } else if (status == "poll") {
                return ServerResponse(success = true, polling = true, message = message)
            } else {
                val errorCode = allData.optInt("error_code", ServerResponse.ERROR_CODE_NO_ERROR_CODE)
                return ServerResponse(
                    success = false,
                    message = message,
                    serverMessageId = messageId,
                    errorCode = errorCode
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Response data: %s", data)
            return ServerResponse(success = false)
        }
    }

    private fun parsePodcastFromJsonObject(podcastJson: JSONObject): Podcast? {
        try {
            val uuid = getString(podcastJson, "uuid") ?: return null
            val podcast = Podcast(
                uuid = uuid,
                title = getString(podcastJson, "title") ?: "",
                thumbnailUrl = getString(podcastJson, "thumbnail_url"),
                podcastDescription = getString(podcastJson, "description") ?: "",
                podcastCategory = getString(podcastJson, "category") ?: "",
                podcastLanguage = getString(podcastJson, "language") ?: "",
                mediaType = getString(podcastJson, "media_type"),
                podcastUrl = getString(podcastJson, "url"),
                author = getString(podcastJson, "author") ?: ""
            )
            // add the episodes
            val episodes = podcastJson.optJSONArray("episodes")
            if (episodes != null) {
                var jsonEpisode: JSONObject
                for (i in 0 until episodes.length()) {
                    jsonEpisode = episodes.getJSONObject(i)
                    parseEpisodeFromJson(jsonEpisode, podcast.uuid)?.let { episode ->
                        podcast.addEpisode(episode)
                    }
                }
            }

            return podcast
        } catch (e: Exception) {
            Timber.e(e, "Problems parsing podcasts.")
            return null // TODO
        }
    }
}
