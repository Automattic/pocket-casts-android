package au.com.shiftyjelly.pocketcasts.repositories.playlist

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.Date
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PlaylistManagerDsl : TestWatcher() {
    private val testDispatcher = StandardTestDispatcher()
    private val clock = MutableClock()

    val episodeLimit = 10

    private lateinit var database: AppDatabase
    private val podcastDao get() = database.podcastDao()
    private val episodeDao get() = database.episodeDao()
    private val playlistDao get() = database.playlistDao()
    private val folderDao get() = database.folderDao()

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var settings: Settings

    lateinit var manager: PlaylistManager
        private set

    override fun starting(description: Description) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val moshi = ServersModule().provideMoshi()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(moshi))
            .setQueryCoroutineContext(testDispatcher)
            .build()
        sharedPrefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        settings = SettingsImpl(
            sharedPreferences = sharedPrefs,
            privatePreferences = sharedPrefs,
            context = context,
            firebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
            moshi = moshi,
        )

        manager = PlaylistManagerImpl(
            appDatabase = database,
            settings = settings,
            clock = clock,
            smartEpisodeLimit = episodeLimit,
            manualEpisodeLimit = episodeLimit,
        )
    }

    override fun finished(description: Description) {
        cleanData()
    }

    fun test(block: suspend PlaylistManagerDsl.() -> Unit) = runTest(testDispatcher) {
        block(this@PlaylistManagerDsl)
    }

    // Setup
    suspend fun <T> transaction(body: suspend () -> T) = database.withTransaction(body)

    suspend fun insertSmartPlaylist(index: Int, builder: (PlaylistEntity) -> PlaylistEntity = { it }): PlaylistEntity {
        val playlist = smartPlaylistEntity(index, builder)
        playlistDao.upsertPlaylist(playlist)
        return playlistDao.getAllPlaylistsIn(listOf(playlist.uuid)).single()
    }

    suspend fun insertManualPlaylist(index: Int, builder: (PlaylistEntity) -> PlaylistEntity = { it }): PlaylistEntity {
        val playlist = manualPlaylistEntity(index, builder)
        playlistDao.upsertPlaylist(playlist)
        return playlistDao.getAllPlaylistsIn(listOf(playlist.uuid)).single()
    }

    suspend fun insertPodcast(index: Int, builder: (Podcast) -> Podcast = { it }): Podcast {
        val podcast = podcast(index, builder)
        podcastDao.insertSuspend(podcast)
        return podcastDao.findPodcastByUuid(podcast.uuid)!!
    }

    suspend fun insertPodcast(index: Int, folderIndex: Int, builder: (Podcast) -> Podcast = { it }): Podcast {
        return insertPodcast(index) {
            builder(it).copy(rawFolderUuid = "folder-id-$folderIndex")
        }
    }

    suspend fun insertPodcastEpisode(index: Int, podcastIndex: Int, builder: (PodcastEpisode) -> PodcastEpisode = { it }): PodcastEpisode {
        val episode = podcastEpisode(index, podcastIndex, builder)
        episodeDao.insert(episode)
        return episodeDao.findByUuid(episode.uuid)!!
    }

    suspend fun updatePodcastEpisode(episode: PodcastEpisode) {
        episodeDao.update(episode)
    }

    suspend fun insertManualEpisode(index: Int, podcastIndex: Int, playlistIndex: Int, builder: (ManualPlaylistEpisode) -> ManualPlaylistEpisode = { it }): ManualPlaylistEpisode {
        val episode = manualPlaylistEpisode(index, podcastIndex, playlistIndex, builder)
        playlistDao.upsertManualEpisode(episode)
        return playlistDao.getManualPlaylistEpisodes(episode.playlistUuid).single { it.episodeUuid == episode.episodeUuid }
    }

    suspend fun deleteManualEpisode(index: Int, playlistIndex: Int) {
        playlistDao.deleteAllManualEpisodesIn("playlist-id-$playlistIndex", listOf("episode-id-$index"))
    }

    suspend fun updateSortType(playlistIndex: Int, type: PlaylistEpisodeSortType) {
        playlistDao.updateSortType("playlist-id-$playlistIndex", type)
    }

    suspend fun insertFolder(index: Int, builder: (Folder) -> Folder = { it }): Folder {
        val folder = folder(index, builder)
        folderDao.insert(folder)
        return folderDao.findByUuid(folder.uuid)!!
    }

    fun setNoSubscription() {
        settings.cachedMembership.set(Membership.Empty, updateModifiedAt = false)
    }

    fun setPlusSubscription() {
        settings.cachedMembership.set(
            Membership.Empty.copy(subscription = Subscription.PlusPreview),
            updateModifiedAt = false,
        )
    }

    fun cleanData() {
        database.clearAllTables()
        sharedPrefs.edit(commit = true) { clear() }
    }

    // Assertions
    suspend fun expectNoPreviews() {
        val actual = manager.playlistPreviewsFlow().first()
        assertTrue(actual.isEmpty())
    }

    suspend fun expectPreviews(preview: PlaylistPreview, vararg previews: PlaylistPreview) {
        val expected = listOf(preview) + previews
        val actual = manager.playlistPreviewsFlow().first()
        assertEquals(expected, actual)
    }

    suspend fun expectPreviewUuids(uuids: List<String>) {
        val actual = manager.playlistPreviewsFlow().first().map(PlaylistPreview::uuid)
        assertEquals(uuids, actual)
    }

    suspend fun expectPreviewEpisodeCount(playlistIndex: Int, count: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = manager.playlistPreviewsFlow().first().singleOrNull { it.uuid == playlistId }
        assertEquals(count, playlist?.episodeCount)
    }

    suspend fun expectPreviewPodcasts(playlistIndex: Int, podcasts: List<String>) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = manager.playlistPreviewsFlow().first().singleOrNull { it.uuid == playlistId }
        assertEquals(podcasts, playlist?.artworkPodcastUuids)
    }

    suspend fun expectPreviewNoPodcasts(playlistIndex: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = manager.playlistPreviewsFlow().first().singleOrNull { it.uuid == playlistId }
        assertEquals(true, playlist?.artworkPodcastUuids?.isEmpty())
    }

    suspend fun expectSyncStatus(playlistIndex: Int, syncStatus: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(syncStatus, playlist?.syncStatus)
    }

    suspend fun expectSortType(playlistIndex: Int, sortType: PlaylistEpisodeSortType) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(sortType, playlist?.sortType)
    }

    suspend fun expectAutoDownloadEnabled(playlistIndex: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(true, playlist?.autoDownload)
    }

    suspend fun expectAutoDownloadDisabled(playlistIndex: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(false, playlist?.autoDownload)
    }

    suspend fun expectAutoDownloadLimit(playlistIndex: Int, limit: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(limit, playlist?.autodownloadLimit)
    }

    suspend fun expectName(playlistIndex: Int, name: String) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(name, playlist?.title)
    }

    suspend fun expectShowArchived(playlistIndex: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(true, playlist?.showArchivedEpisodes)
    }

    suspend fun expectNotShowArchived(playlistIndex: Int) {
        val playlistId = "playlist-id-$playlistIndex"
        val playlist = playlistDao.getAllPlaylistsIn(listOf(playlistId)).singleOrNull()
        assertEquals(false, playlist?.showArchivedEpisodes)
    }

    suspend fun expectPlaylist(playlist: PlaylistEntity) {
        val actual = playlistDao.getAllPlaylistsIn(listOf(playlist.uuid)).singleOrNull()
        assertEquals(playlist, actual)
    }

    suspend fun expectManualEpisodes(playlistIndex: Int, episode: ManualPlaylistEpisode, vararg episodes: ManualPlaylistEpisode) {
        val actual = playlistDao.getManualPlaylistEpisodes("playlist-id-$playlistIndex")
        val expected = listOf(episode) + episodes
        assertEquals(expected, actual)
    }

    suspend fun expectNoManualEpisodes(playlistIndex: Int) {
        val actual = playlistDao.getManualPlaylistEpisodes("playlist-id-$playlistIndex")
        assertTrue(actual.isEmpty())
    }

    suspend fun expectNoManualEpisodesCount(playlistIndex: Int, count: Int) {
        val actual = playlistDao.getManualPlaylistEpisodes("playlist-id-$playlistIndex")
        assertEquals(count, actual.size)
    }

    // Creators
    fun smartPlaylistEntity(index: Int, builder: (PlaylistEntity) -> PlaylistEntity = { it }): PlaylistEntity {
        val id = "playlist-id-$index"
        return builder(
            PlaylistEntity(
                id = index.toLong() + 1,
                uuid = id,
                title = "Playlist title $index",
            ),
        ).copy(manual = false)
    }

    fun manualPlaylistEntity(index: Int, builder: (PlaylistEntity) -> PlaylistEntity = { it }): PlaylistEntity {
        val id = "playlist-id-$index"
        return builder(
            PlaylistEntity(
                id = index.toLong() + 1,
                uuid = id,
                title = "Playlist title $index",
                sortType = PlaylistEpisodeSortType.DragAndDrop,
            ),
        ).copy(manual = true)
    }

    fun smartPreview(index: Int, builder: (SmartPlaylistPreview) -> (SmartPlaylistPreview) = { it }): SmartPlaylistPreview {
        val preview = builder(
            SmartPlaylistPreview(
                uuid = "playlist-id-$index",
                title = "Playlist title $index",
                episodeCount = 0,
                artworkPodcastUuids = emptyList(),
                settings = Playlist.Settings(
                    sortType = PlaylistEpisodeSortType.NewestToOldest,
                    isAutoDownloadEnabled = false,
                    autoDownloadLimit = 10,
                ),
                smartRules = SmartRules.Default,
                icon = PlaylistIcon(0),
            ),
        )
        return preview
    }

    fun manualPreview(index: Int, builder: (ManualPlaylistPreview) -> (ManualPlaylistPreview) = { it }): ManualPlaylistPreview {
        val preview = builder(
            ManualPlaylistPreview(
                uuid = "playlist-id-$index",
                title = "Playlist title $index",
                episodeCount = 0,
                artworkPodcastUuids = emptyList(),
                settings = Playlist.Settings(
                    sortType = PlaylistEpisodeSortType.DragAndDrop,
                    isAutoDownloadEnabled = false,
                    autoDownloadLimit = 10,
                ),
                icon = PlaylistIcon(0),
            ),
        )
        return preview
    }

    fun podcast(index: Int, builder: (Podcast) -> Podcast = { it }): Podcast {
        return builder(
            Podcast(
                uuid = "podcast-id-$index",
                title = "Podcast title $index",
                author = "Podcast author $index",
                slug = "podcast-slug-$index",
                isSubscribed = true,
            ),
        )
    }

    fun podcastEpisode(index: Int, podcastIndex: Int, builder: (PodcastEpisode) -> PodcastEpisode = { it }): PodcastEpisode {
        return builder(
            PodcastEpisode(
                uuid = "episode-id-$index",
                title = "Episode title $index",
                podcastUuid = "podcast-id-$podcastIndex",
                publishedDate = Date(Long.MAX_VALUE - index),
                slug = "episode-slug-$index",
                addedDate = Date.from(clock.instant()),
            ),
        )
    }

    fun manualPlaylistEpisode(index: Int, podcastIndex: Int, playlistIndex: Int, builder: (ManualPlaylistEpisode) -> ManualPlaylistEpisode = { it }): ManualPlaylistEpisode {
        return builder(
            ManualPlaylistEpisode(
                episodeUuid = "episode-id-$index",
                podcastUuid = "podcast-id-$podcastIndex",
                playlistUuid = "playlist-id-$playlistIndex",
                title = "Episode title $index",
                addedAt = clock.instant(),
                publishedAt = Date(Long.MAX_VALUE - index).toInstant(),
                downloadUrl = null,
                episodeSlug = "episode-slug-$index",
                podcastSlug = "podcast-slug-$podcastIndex",
                sortPosition = index,
                isSynced = true,
            ),
        )
    }

    fun playlistDraft(index: Int, builder: (SmartRules) -> SmartRules = { it }): SmartPlaylistDraft {
        return SmartPlaylistDraft(
            title = "Playlist title $index",
            rules = smartRules(builder),
        )
    }

    fun smartRules(builder: (SmartRules) -> SmartRules = { it }): SmartRules {
        return builder(SmartRules.Default)
    }

    fun smartPlaylist(index: Int, builder: (SmartPlaylist) -> SmartPlaylist = { it }): SmartPlaylist {
        return builder(
            SmartPlaylist(
                uuid = "playlist-id-$index",
                title = "Playlist title $index",
                smartRules = SmartRules.Default,
                episodes = emptyList(),
                settings = Playlist.Settings(
                    sortType = PlaylistEpisodeSortType.NewestToOldest,
                    isAutoDownloadEnabled = false,
                    autoDownloadLimit = 10,
                ),
                metadata = Playlist.Metadata(
                    playbackDurationLeft = Duration.ZERO,
                    artworkUuids = emptyList(),
                    isShowingArchived = false,
                    totalEpisodeCount = 0,
                    archivedEpisodeCount = 0,
                    displayedEpisodeCount = 0,
                    displayedAvailableEpisodeCount = 0,
                ),
            ),
        )
    }

    fun manualPlaylist(index: Int, builder: (ManualPlaylist) -> ManualPlaylist = { it }): ManualPlaylist {
        return builder(
            ManualPlaylist(
                uuid = "playlist-id-$index",
                title = "Playlist title $index",
                episodes = emptyList(),
                settings = Playlist.Settings(
                    sortType = PlaylistEpisodeSortType.DragAndDrop,
                    isAutoDownloadEnabled = false,
                    autoDownloadLimit = 10,
                ),
                metadata = Playlist.Metadata(
                    playbackDurationLeft = Duration.ZERO,
                    artworkUuids = emptyList(),
                    isShowingArchived = false,
                    totalEpisodeCount = 0,
                    archivedEpisodeCount = 0,
                    displayedEpisodeCount = 0,
                    displayedAvailableEpisodeCount = 0,
                ),
            ),
        )
    }

    fun folder(index: Int, builder: (Folder) -> Folder = { it }): Folder {
        return builder(
            Folder(
                uuid = "folder-id-$index",
                name = "Folder title $index",
                color = 0,
                addedDate = Date(0),
                sortPosition = 0,
                podcastsSortType = PodcastsSortType.RECENTLY_PLAYED,
                deleted = false,
                syncModified = 0L,
            ),
        )
    }

    fun podcastEpisodeSource(index: Int, builder: (ManualPlaylistPodcastSource) -> ManualPlaylistPodcastSource = { it }): ManualPlaylistPodcastSource {
        return builder(
            ManualPlaylistPodcastSource(
                uuid = "podcast-id-$index",
                title = "Podcast title $index",
                author = "Podcast author $index",
            ),
        )
    }

    fun folderEpisodeSource(index: Int, podcastIndices: List<Int>, builder: (ManualPlaylistFolderSource) -> ManualPlaylistFolderSource = { it }): ManualPlaylistFolderSource {
        return builder(
            ManualPlaylistFolderSource(
                uuid = "folder-id-$index",
                title = "Folder title $index",
                color = 0,
                podcastSources = podcastIndices.map { podcastIndex -> "podcast-id-$podcastIndex" },
            ),
        )
    }

    fun availablePlaylistEpisode(index: Int, podcastIndex: Int, builder: (PodcastEpisode) -> PodcastEpisode = { it }): PlaylistEpisode.Available {
        return PlaylistEpisode.Available(podcastEpisode(index, podcastIndex, builder))
    }

    fun unavailableManualEpisode(index: Int, podcastIndex: Int, playlistIndex: Int, builder: (ManualPlaylistEpisode) -> ManualPlaylistEpisode = { it }): PlaylistEpisode.Unavailable {
        return PlaylistEpisode.Unavailable(manualPlaylistEpisode(index, podcastIndex, playlistIndex, builder))
    }

    fun playlistPreviewForEpisode(index: Int, builder: (PlaylistPreviewForEpisode) -> PlaylistPreviewForEpisode = { it }): PlaylistPreviewForEpisode {
        return builder(
            PlaylistPreviewForEpisode(
                uuid = "playlist-id-$index",
                title = "Playlist title $index",
                episodeCount = 0,
                artworkPodcastUuids = emptyList(),
                hasEpisode = false,
                episodeLimit = episodeLimit,
            ),
        )
    }
}
