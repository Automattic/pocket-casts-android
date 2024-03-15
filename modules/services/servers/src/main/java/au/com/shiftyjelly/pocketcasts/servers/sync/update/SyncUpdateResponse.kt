package au.com.shiftyjelly.pocketcasts.servers.sync.update

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.pocketcasts.service.api.SyncUserBookmark
import com.pocketcasts.service.api.SyncUserEpisode
import com.pocketcasts.service.api.SyncUserFolder
import com.pocketcasts.service.api.SyncUserPlaylist
import com.pocketcasts.service.api.SyncUserPodcast
import com.pocketcasts.service.api.UserPodcastResponse
import com.pocketcasts.service.api.addToUpNextOrNull
import com.pocketcasts.service.api.addToUpNextPositionOrNull
import com.pocketcasts.service.api.autoArchiveEpisodeLimitOrNull
import com.pocketcasts.service.api.autoArchiveInactiveOrNull
import com.pocketcasts.service.api.autoArchiveOrNull
import com.pocketcasts.service.api.autoArchivePlayedOrNull
import com.pocketcasts.service.api.autoSkipLastOrNull
import com.pocketcasts.service.api.autoStartFromOrNull
import com.pocketcasts.service.api.bookmarkOrNull
import com.pocketcasts.service.api.dateAddedOrNull
import com.pocketcasts.service.api.durationOrNull
import com.pocketcasts.service.api.episodeGroupingOrNull
import com.pocketcasts.service.api.episodeOrNull
import com.pocketcasts.service.api.episodesSortOrderOrNull
import com.pocketcasts.service.api.folderOrNull
import com.pocketcasts.service.api.folderUuidOrNull
import com.pocketcasts.service.api.isDeletedOrNull
import com.pocketcasts.service.api.notificationOrNull
import com.pocketcasts.service.api.playbackEffectsOrNull
import com.pocketcasts.service.api.playbackSpeedOrNull
import com.pocketcasts.service.api.playedUpToOrNull
import com.pocketcasts.service.api.playingStatusOrNull
import com.pocketcasts.service.api.playlistOrNull
import com.pocketcasts.service.api.podcastOrNull
import com.pocketcasts.service.api.sortPositionOrNull
import com.pocketcasts.service.api.starredOrNull
import com.pocketcasts.service.api.subscribedOrNull
import com.pocketcasts.service.api.trimSilenceOrNull
import com.pocketcasts.service.api.volumeBoostOrNull
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

data class SyncUpdateResponse(
    var lastModified: String? = null,
    var token: String? = null,
    val playlists: MutableList<Playlist> = mutableListOf(),
    val episodes: MutableList<EpisodeSync> = mutableListOf(),
    val podcasts: MutableList<PodcastSync> = mutableListOf(),
    val folders: MutableList<Folder> = mutableListOf(),
    val bookmarks: MutableList<Bookmark> = mutableListOf(),
) {

    data class PodcastSync(
        var uuid: String? = null,
        var subscribed: Boolean = false,
        var folderUuid: String? = null,
        var sortPosition: Int? = null,
        var dateAdded: Date? = null,
        var episodesSortOrder: Int? = null,
        var episodesSortOrderModified: Date? = null,
        var startFromSecs: Int? = null,
        var startFromModified: Date? = null,
        var skipLastSecs: Int? = null,
        var skipLastModified: Date? = null,
        var addToUpNext: Boolean? = null,
        var addToUpNextModified: Date? = null,
        var addToUpNextPosition: Int? = null,
        var addToUpNextPositionModified: Date? = null,
        var useCustomPlaybackEffects: Boolean? = null,
        var useCustomPlaybackEffectsModified: Date? = null,
        var playbackSpeed: Double? = null,
        var playbackSpeedModified: Date? = null,
        var trimSilence: Int? = null,
        var trimSilenceModified: Date? = null,
        var useVolumeBoost: Boolean? = null,
        var useVolumeBoostModified: Date? = null,
        var showNotifications: Boolean? = null,
        var showNotificationsModified: Date? = null,
        var autoArchive: Boolean? = null,
        var autoArchiveModified: Date? = null,
        var autoArchivePlayed: Int? = null,
        var autoArchivePlayedModified: Date? = null,
        var autoArchiveInactive: Int? = null,
        var autoArchiveInactiveModified: Date? = null,
        var autoArchiveEpisodeLimit: Int? = null,
        var autoArchiveEpisodeLimitModified: Date? = null,
        var episodeGrouping: Int? = null,
        var episodeGroupingModified: Date? = null,
        var showArchived: Boolean? = null,
        var showArchivedModified: Date? = null,
    ) {
        companion object {
            fun fromSyncUserPodcast(syncUserPodcast: SyncUserPodcast): PodcastSync =
                PodcastSync(
                    uuid = syncUserPodcast.uuid,
                    subscribed = syncUserPodcast.subscribedOrNull?.value ?: false,
                    folderUuid = syncUserPodcast.folderUuidOrNull?.value,
                    sortPosition = syncUserPodcast.sortPositionOrNull?.value,
                    dateAdded = syncUserPodcast.dateAddedOrNull?.toDate(),
                    episodesSortOrder = syncUserPodcast.settings.episodesSortOrderOrNull?.value?.value,
                    episodesSortOrderModified = syncUserPodcast.settings.episodesSortOrderOrNull?.modifiedAt?.toDate(),
                    startFromSecs = syncUserPodcast.settings.autoStartFromOrNull?.value?.value,
                    startFromModified = syncUserPodcast.settings.autoStartFromOrNull?.modifiedAt?.toDate(),
                    skipLastSecs = syncUserPodcast.settings.autoSkipLastOrNull?.value?.value,
                    skipLastModified = syncUserPodcast.settings.autoSkipLastOrNull?.modifiedAt?.toDate(),
                    addToUpNext = syncUserPodcast.settings.addToUpNextOrNull?.value?.value,
                    addToUpNextModified = syncUserPodcast.settings.addToUpNextOrNull?.modifiedAt?.toDate(),
                    addToUpNextPosition = syncUserPodcast.settings.addToUpNextPositionOrNull?.value?.value,
                    addToUpNextPositionModified = syncUserPodcast.settings.addToUpNextPositionOrNull?.modifiedAt?.toDate(),
                    useCustomPlaybackEffects = syncUserPodcast.settings.playbackEffectsOrNull?.value?.value,
                    useCustomPlaybackEffectsModified = syncUserPodcast.settings.playbackEffectsOrNull?.modifiedAt?.toDate(),
                    playbackSpeed = syncUserPodcast.settings.playbackSpeedOrNull?.value?.value,
                    playbackSpeedModified = syncUserPodcast.settings.playbackSpeedOrNull?.modifiedAt?.toDate(),
                    trimSilence = syncUserPodcast.settings.trimSilenceOrNull?.value?.value,
                    trimSilenceModified = syncUserPodcast.settings.trimSilenceOrNull?.modifiedAt?.toDate(),
                    useVolumeBoost = syncUserPodcast.settings.volumeBoostOrNull?.value?.value,
                    useVolumeBoostModified = syncUserPodcast.settings.volumeBoostOrNull?.modifiedAt?.toDate(),
                    showNotifications = syncUserPodcast.settings.notificationOrNull?.value?.value,
                    showNotificationsModified = syncUserPodcast.settings.notificationOrNull?.modifiedAt?.toDate(),
                    autoArchive = syncUserPodcast.settings.autoArchiveOrNull?.value?.value,
                    autoArchiveModified = syncUserPodcast.settings.autoArchiveOrNull?.modifiedAt?.toDate(),
                    autoArchivePlayed = syncUserPodcast.settings.autoArchivePlayedOrNull?.value?.value,
                    autoArchivePlayedModified = syncUserPodcast.settings.autoArchivePlayedOrNull?.modifiedAt?.toDate(),
                    autoArchiveInactive = syncUserPodcast.settings.autoArchiveInactiveOrNull?.value?.value,
                    autoArchiveInactiveModified = syncUserPodcast.settings.autoArchiveInactiveOrNull?.modifiedAt?.toDate(),
                    autoArchiveEpisodeLimit = syncUserPodcast.settings.autoArchiveEpisodeLimitOrNull?.value?.value,
                    autoArchiveEpisodeLimitModified = syncUserPodcast.settings.autoArchiveEpisodeLimitOrNull?.modifiedAt?.toDate(),
                    episodeGrouping = syncUserPodcast.settings.episodeGroupingOrNull?.value?.value,
                    episodeGroupingModified = syncUserPodcast.settings.episodesSortOrderOrNull?.modifiedAt?.toDate(),
                    showArchived = syncUserPodcast.settings.showArchived?.value?.value,
                    showArchivedModified = syncUserPodcast.settings.showArchived?.modifiedAt?.toDate(),
                )

            fun fromUserPodcastResponse(response: UserPodcastResponse) = PodcastSync(
                uuid = response.uuid,
                subscribed = true,
                folderUuid = response.folderUuidOrNull?.value,
                sortPosition = response.sortPositionOrNull?.value,
                dateAdded = response.dateAddedOrNull?.toDate(),
                episodesSortOrder = response.settings.episodesSortOrderOrNull?.value?.value,
                episodesSortOrderModified = response.settings.episodesSortOrderOrNull?.modifiedAt?.toDate(),
                startFromSecs = response.settings.autoStartFromOrNull?.value?.value,
                startFromModified = response.settings.autoStartFromOrNull?.modifiedAt?.toDate(),
                skipLastSecs = response.settings.autoSkipLastOrNull?.value?.value,
                skipLastModified = response.settings.autoSkipLastOrNull?.modifiedAt?.toDate(),
                addToUpNext = response.settings.addToUpNextOrNull?.value?.value,
                addToUpNextModified = response.settings.addToUpNextOrNull?.modifiedAt?.toDate(),
                addToUpNextPosition = response.settings.addToUpNextPositionOrNull?.value?.value,
                addToUpNextPositionModified = response.settings.addToUpNextPositionOrNull?.modifiedAt?.toDate(),
                useCustomPlaybackEffects = response.settings.playbackEffectsOrNull?.value?.value,
                useCustomPlaybackEffectsModified = response.settings.playbackEffectsOrNull?.modifiedAt?.toDate(),
                playbackSpeed = response.settings.playbackSpeedOrNull?.value?.value,
                playbackSpeedModified = response.settings.playbackSpeedOrNull?.modifiedAt?.toDate(),
                trimSilence = response.settings.trimSilenceOrNull?.value?.value,
                trimSilenceModified = response.settings.trimSilenceOrNull?.modifiedAt?.toDate(),
                useVolumeBoost = response.settings.volumeBoostOrNull?.value?.value,
                useVolumeBoostModified = response.settings.volumeBoostOrNull?.modifiedAt?.toDate(),
                showNotifications = response.settings.notificationOrNull?.value?.value,
                showNotificationsModified = response.settings.notificationOrNull?.modifiedAt?.toDate(),
                autoArchive = response.settings.autoArchiveOrNull?.value?.value,
                autoArchiveModified = response.settings.autoArchiveOrNull?.modifiedAt?.toDate(),
                autoArchivePlayed = response.settings.autoArchivePlayedOrNull?.value?.value,
                autoArchivePlayedModified = response.settings.autoArchivePlayedOrNull?.modifiedAt?.toDate(),
                autoArchiveInactive = response.settings.autoArchiveInactiveOrNull?.value?.value,
                autoArchiveInactiveModified = response.settings.autoArchiveInactiveOrNull?.modifiedAt?.toDate(),
                autoArchiveEpisodeLimit = response.settings.autoArchiveEpisodeLimitOrNull?.value?.value,
                autoArchiveEpisodeLimitModified = response.settings.autoArchiveEpisodeLimitOrNull?.modifiedAt?.toDate(),
                episodeGrouping = response.settings.episodeGroupingOrNull?.value?.value,
                episodeGroupingModified = response.settings.episodesSortOrderOrNull?.modifiedAt?.toDate(),
                showArchived = response.settings.showArchived?.value?.value,
                showArchivedModified = response.settings.showArchived?.modifiedAt?.toDate(),
            )
        }
    }

    data class EpisodeSync(
        var uuid: String? = null,
        var isArchived: Boolean? = null,
        var starred: Boolean? = null,
        var playedUpTo: Double? = null,
        var duration: Double? = null,
        var playingStatus: EpisodePlayingStatus? = null,
        var deselectedChapters: ChapterIndices? = null,
        var deselectedChaptersModified: Long? = null,
    ) {
        companion object {
            fun fromSyncUserEpisode(syncUserEpisode: SyncUserEpisode): EpisodeSync =
                EpisodeSync(
                    uuid = syncUserEpisode.uuid,
                    isArchived = syncUserEpisode.isDeletedOrNull?.value ?: false,
                    starred = syncUserEpisode.starredOrNull?.value ?: false,
                    playedUpTo = syncUserEpisode.playedUpToOrNull?.value?.toDouble(),
                    duration = syncUserEpisode.durationOrNull?.value?.toDouble(),
                    playingStatus = syncUserEpisode.playingStatusOrNull?.value?.let {
                        EpisodePlayingStatus.fromInt(it)
                    } ?: EpisodePlayingStatus.NOT_PLAYED,
                    deselectedChapters = ChapterIndices.fromString(syncUserEpisode.deselectedChapters),
                    deselectedChaptersModified = syncUserEpisode.deselectedChaptersModified.value,
                )
        }
    }

    companion object {
        fun fromProtobufSyncUpdateResponse(
            source: com.pocketcasts.service.api.SyncUpdateResponse,
        ): SyncUpdateResponse =
            SyncUpdateResponse(
                lastModified = run {
                    val epochMilli = source.lastModified
                    val instant = Instant.ofEpochMilli(epochMilli)
                    DateTimeFormatter.ISO_INSTANT.format(instant)
                },
                podcasts = source.recordsList
                    .mapNotNull { it.podcastOrNull }
                    .map { PodcastSync.fromSyncUserPodcast(it) }
                    .toMutableList(),
                episodes = source.recordsList
                    .mapNotNull { it.episodeOrNull }
                    .map { EpisodeSync.fromSyncUserEpisode(it) }
                    .toMutableList(),
                folders = source.recordsList
                    .mapNotNull { it.folderOrNull }
                    .mapNotNull { syncUserFolderToFolder(it) }
                    .toMutableList(),
                playlists = source.recordsList
                    .mapNotNull { it.playlistOrNull }
                    .map { syncUserPlaylistToPlaylist(it) }
                    .toMutableList(),
                bookmarks = source.recordsList
                    .mapNotNull { it.bookmarkOrNull }
                    .map { syncUserBookmarkToBookmark(it) }
                    .toMutableList(),
            )
    }
}

private fun syncUserFolderToFolder(syncUserFolder: SyncUserFolder): Folder? =
    syncUserFolder.dateAddedOrNull?.toDate()?.let { syncUserFolderDateAdded ->
        Folder(
            uuid = syncUserFolder.folderUuid,
            name = syncUserFolder.name,
            sortPosition = syncUserFolder.sortPosition,
            addedDate = syncUserFolderDateAdded,
            color = syncUserFolder.color,
            podcastsSortType = PodcastsSortType.fromServerId(syncUserFolder.podcastsSortType),
            deleted = syncUserFolder.isDeleted,
            syncModified = Folder.SYNC_MODIFIED_FROM_SERVER,
        )
    } ?: run {
        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "unable to parse dateAdded from syncUserFolder: dateAdded: ${syncUserFolder.dateAddedOrNull}, folder name: ${syncUserFolder.name}")
        null
    }

private fun syncUserPlaylistToPlaylist(syncUserPlaylist: SyncUserPlaylist): Playlist =
    Playlist(
        uuid = syncUserPlaylist.originalUuid, // it's important to use originalUuid, not uuid because the server won't change the upper/lower case of this one
        deleted = syncUserPlaylist.isDeleted.value,
        title = syncUserPlaylist.title.value,
        audioVideo = syncUserPlaylist.audioVideo.value,
        notDownloaded = syncUserPlaylist.notDownloaded.value,
        downloaded = syncUserPlaylist.downloaded.value,
        downloading = syncUserPlaylist.downloading.value,
        finished = syncUserPlaylist.finished.value,
        partiallyPlayed = syncUserPlaylist.partiallyPlayed.value,
        unplayed = syncUserPlaylist.unplayed.value,
        starred = syncUserPlaylist.starred.value,
        manual = syncUserPlaylist.manual.value,
        sortPosition = syncUserPlaylist.sortPosition.value,
        sortId = syncUserPlaylist.sortType.value,
        iconId = syncUserPlaylist.iconId.value,
        allPodcasts = syncUserPlaylist.allPodcasts.value,
        podcastUuids = syncUserPlaylist.podcastUuids.value,
        filterHours = syncUserPlaylist.filterHours.value,
        syncStatus = Playlist.SYNC_STATUS_SYNCED,
        filterDuration = syncUserPlaylist.filterDuration.value,
        longerThan = syncUserPlaylist.longerThan.value,
        shorterThan = syncUserPlaylist.shorterThan.value,
    )

private fun syncUserBookmarkToBookmark(syncUserBookmark: SyncUserBookmark): Bookmark =
    Bookmark(
        uuid = syncUserBookmark.bookmarkUuid,
        episodeUuid = syncUserBookmark.episodeUuid,
        podcastUuid = syncUserBookmark.podcastUuid,
        timeSecs = syncUserBookmark.time.value,
        createdAt = syncUserBookmark.createdAt.toDate() ?: run {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Unable to parse createdAt from SyncUserBookmark: createdAt: ${syncUserBookmark.createdAt}, bookmarkUuid: ${syncUserBookmark.bookmarkUuid}")
            Date()
        },
        title = syncUserBookmark.title.value,
        titleModified = syncUserBookmark.titleModified.value,
        deleted = syncUserBookmark.isDeleted.value,
        deletedModified = syncUserBookmark.isDeletedModified.value,
        syncStatus = SyncStatus.SYNCED,

    )
