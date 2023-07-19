package au.com.shiftyjelly.pocketcasts.servers.sync.update

import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.servers.extensions.nextBooleanOrDefault
import au.com.shiftyjelly.pocketcasts.servers.extensions.nextBooleanOrNull
import au.com.shiftyjelly.pocketcasts.servers.extensions.nextDoubleOrNull
import au.com.shiftyjelly.pocketcasts.servers.extensions.nextIntOrDefault
import au.com.shiftyjelly.pocketcasts.servers.extensions.nextIntOrNull
import au.com.shiftyjelly.pocketcasts.servers.extensions.nextStringOrNull
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.util.Date

class SyncUpdateResponseParser : JsonAdapter<SyncUpdateResponse>() {

    @ToJson
    override fun toJson(writer: JsonWriter, value: SyncUpdateResponse?) {}

    @FromJson
    override fun fromJson(reader: JsonReader): SyncUpdateResponse {
        val response = SyncUpdateResponse()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "token" -> response.token = reader.nextStringOrNull()
                "result" -> readResult(reader, response)
                // Until we switch to the new user sync API and use HTTP status codes assume the error code of 2 means the token has expired but the user could still be logged in
                "error_code" -> if (reader.nextInt() == 2) throw HttpException(Response.error<ResponseBody>(401, "".toResponseBody("plain/text".toMediaTypeOrNull())))
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return response
    }

    private fun readResult(reader: JsonReader, response: SyncUpdateResponse) {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.skipValue()
            return
        }

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "last_modified" -> response.lastModified = reader.nextStringOrNull()
                "changes" -> readChanges(reader, response)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun readChanges(reader: JsonReader, response: SyncUpdateResponse) {
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            var type: String? = null
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "type" -> type = reader.nextStringOrNull()
                    "fields" -> readField(type, reader, response)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        reader.endArray()
    }

    private fun readField(type: String?, reader: JsonReader, response: SyncUpdateResponse) {
        when (type) {
            "UserPlaylist" -> readPlaylist(reader, response)
            "UserFolder" -> readFolder(reader, response)
            "UserPodcast" -> readPodcast(reader, response)
            "UserEpisode" -> readEpisode(reader, response)
            "UserBookmark" -> readBookmark(reader, response)
            null -> throw Exception("No type found for field")
            else -> reader.skipValue()
        }
    }

    private fun readEpisode(reader: JsonReader, response: SyncUpdateResponse) {
        val episode = SyncUpdateResponse.EpisodeSync()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "uuid" -> episode.uuid = reader.nextString()
                "starred" -> episode.starred = reader.nextBooleanOrNull()
                "played_up_to" -> episode.playedUpTo = reader.nextDoubleOrNull()
                "duration" -> episode.duration = reader.nextDoubleOrNull()
                "playing_status" -> episode.playingStatus = readPlayingStatus(reader)
                "is_deleted" -> episode.isArchived = reader.nextBooleanOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        response.episodes.add(episode)
    }

    private fun readPlayingStatus(reader: JsonReader): EpisodePlayingStatus? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        return when (reader.nextInt()) {
            1 -> EpisodePlayingStatus.NOT_PLAYED
            2 -> EpisodePlayingStatus.IN_PROGRESS
            3 -> EpisodePlayingStatus.COMPLETED
            else -> null
        }
    }

    private fun readPodcast(reader: JsonReader, response: SyncUpdateResponse) {
        val podcast = SyncUpdateResponse.PodcastSync()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "uuid" -> podcast.uuid = reader.nextStringOrNull()
                "is_deleted" -> podcast.subscribed = !reader.nextBoolean()
                "auto_start_from" -> podcast.startFromSecs = reader.nextIntOrNull()
                "episodes_sort_order" -> podcast.episodesSortOrder = reader.nextIntOrNull()
                "auto_skip_last" -> podcast.skipLastSecs = reader.nextIntOrNull()
                "folder_uuid" -> {
                    val folderUuid = reader.nextStringOrNull()
                    podcast.folderUuid = if (folderUuid == null || folderUuid == Folder.homeFolderUuid) null else folderUuid
                }
                "sort_position" -> podcast.sortPosition = reader.nextIntOrNull()
                "date_added" -> podcast.dateAdded = reader.nextStringOrNull()?.parseIsoDate()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        response.podcasts.add(podcast)
    }

    private fun readPlaylist(reader: JsonReader, response: SyncUpdateResponse) {
        val playlist = Playlist()
        playlist.allPodcasts = true
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "uuid" -> playlist.uuid = reader.nextStringOrNull() ?: ""
                "title" -> playlist.title = reader.nextStringOrNull() ?: ""
                "all_podcasts" -> playlist.allPodcasts = reader.nextBooleanOrDefault(true)
                "podcast_uuids" -> playlist.podcastUuids = reader.nextStringOrNull()
                "audio_video" -> playlist.audioVideo = reader.nextIntOrDefault(0)
                "not_downloaded" -> playlist.notDownloaded = reader.nextBooleanOrDefault(false)
                "downloaded" -> playlist.downloaded = reader.nextBooleanOrDefault(false)
                "downloading" -> playlist.downloading = reader.nextBooleanOrDefault(false)
                "finished" -> playlist.finished = reader.nextBooleanOrDefault(false)
                "partially_played" -> playlist.partiallyPlayed = reader.nextBooleanOrDefault(false)
                "unplayed" -> playlist.unplayed = reader.nextBooleanOrDefault(false)
                "starred" -> playlist.starred = reader.nextBooleanOrDefault(false)
                "manual" -> playlist.manual = reader.nextBooleanOrDefault(false)
                "episode_uuids" -> playlist.episodeUuids = reader.nextStringOrNull()
                "sort_position" -> playlist.sortPosition = reader.nextIntOrDefault(0)
                "sort_type" -> playlist.sortId = reader.nextIntOrDefault(0)
                "icon_id" -> playlist.iconId = reader.nextIntOrDefault(0)
                "is_deleted" -> playlist.deleted = reader.nextBooleanOrDefault(false)
                "filter_hours" -> playlist.filterHours = reader.nextIntOrDefault(0)
                "filter_duration" -> playlist.filterDuration = reader.nextBooleanOrDefault(false)
                "shorter_than" -> playlist.shorterThan = reader.nextIntOrDefault(0)
                "longer_than" -> playlist.longerThan = reader.nextIntOrDefault(0)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        response.playlists.add(playlist)
    }

    private fun readFolder(reader: JsonReader, response: SyncUpdateResponse) {
        var uuid: String? = null
        var deleted: Boolean? = null
        var name: String? = null
        var color: Int? = null
        var sortPosition: Int? = null
        var podcastsSortType: PodcastsSortType? = null
        var addedDate: Date? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "folder_uuid" -> uuid = reader.nextStringOrNull()
                "is_deleted" -> deleted = reader.nextBooleanOrNull()
                "name" -> name = reader.nextStringOrNull()
                "color" -> color = reader.nextIntOrNull()
                "sort_position" -> sortPosition = reader.nextIntOrNull()
                "podcasts_sort_type" -> podcastsSortType = PodcastsSortType.fromServerId(reader.nextIntOrNull())
                "date_added" -> addedDate = reader.nextStringOrNull()?.parseIsoDate()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (uuid != null && name != null && color != null && sortPosition != null && podcastsSortType != null && deleted != null && addedDate != null) {
            val folder = Folder(
                uuid = uuid,
                name = name,
                color = color,
                addedDate = addedDate,
                sortPosition = sortPosition,
                podcastsSortType = podcastsSortType,
                deleted = deleted,
                syncModified = 0
            )
            response.folders.add(folder)
        }
    }

    private fun readBookmark(reader: JsonReader, response: SyncUpdateResponse) {
        if (!FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)) {
            return
        }

        var uuid: String? = null
        var podcastUuid: String? = null
        var episodeUuid: String? = null
        var time: Int? = null
        var title: String? = null
        var deleted: Boolean? = null
        var createdAt: Date? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "bookmark_uuid" -> uuid = reader.nextString()
                "podcast_uuid" -> podcastUuid = reader.nextString()
                "episode_uuid" -> episodeUuid = reader.nextString()
                "time" -> time = reader.nextIntOrNull()
                "title" -> title = reader.nextString()
                "is_deleted" -> deleted = reader.nextBooleanOrNull()
                "created_at" -> createdAt = reader.nextStringOrNull()?.parseIsoDate()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (uuid != null && podcastUuid != null && episodeUuid != null && time != null && title != null && deleted != null && createdAt != null) {
            val bookmark = Bookmark(
                uuid = uuid,
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                timeSecs = time,
                title = title,
                deleted = deleted,
                createdAt = createdAt,
                syncStatus = SyncStatus.SYNCED
            )
            response.bookmarks.add(bookmark)
        }
    }
}
