package au.com.shiftyjelly.pocketcasts.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.calculateCombinedIconId
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class CreateFilterViewModel @Inject constructor(val playlistManager: PlaylistManager, val episodeManager: EpisodeManager, val playbackManager: PlaybackManager) :
    ViewModel(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var hasBeenInitialised = false
    lateinit var playlist: LiveData<Playlist>
    val lockedToFirstPage = MutableLiveData<Boolean>(true)

    suspend fun createFilter(name: String, iconId: Int, colorId: Int) =
        withContext(Dispatchers.IO) { playlistManager.createPlaylist(name, Playlist.calculateCombinedIconId(colorId, iconId), draft = true) }

    val filterName = MutableLiveData("")
    var iconId: Int = 0
    var colorId = MutableLiveData(0)

    fun saveNewFilterDetails() {
        val colorId = colorId.value ?: return
        launch {
            saveFilter(iconId, colorId)
            withContext(Dispatchers.Main) {
                reset()
            }
        }
    }

    suspend fun saveFilter(iconId: Int, colorId: Int) = withContext(Dispatchers.Default) {
        val playlist = playlist.value ?: return@withContext
        playlist.title = filterName.value ?: ""
        playlist.iconId = Playlist.calculateCombinedIconId(colorId, iconId)
        playlist.draft = false
        playlistManager.update(playlist)
    }

    fun updateAutodownload(autoDownload: Boolean) {
        launch {
            playlist.value?.let { playlist ->
                playlist.autoDownload = autoDownload
                playlistManager.update(playlist)
            }
        }
    }

    suspend fun setup(playlistUUID: String?) {
        if (hasBeenInitialised) {
            return
        }

        playlist = if (playlistUUID != null) {
            LiveDataReactiveStreams.fromPublisher(playlistManager.findByUuidRx(playlistUUID).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).toFlowable())
        } else {
            val newFilter = createFilter("", 0, 0)
            LiveDataReactiveStreams.fromPublisher(playlistManager.observeByUuid(newFilter.uuid))
        }

        hasBeenInitialised = true
    }

    fun observeFilter(filter: Playlist): LiveData<List<Episode>> {
        return LiveDataReactiveStreams.fromPublisher(playlistManager.observeEpisodesPreview(filter, episodeManager, playbackManager))
    }

    fun updateDownloadLimit(limit: Int) {
        launch {
            val playlist = playlist.value ?: return@launch
            playlist.autodownloadLimit = limit
            playlistManager.update(playlist)
        }
    }

    fun reset() {
        filterName.value = ""
        iconId = 0
        colorId.value = 0
        hasBeenInitialised = false
        lockedToFirstPage.value = true
    }

    fun clearNewFilter() {
        reset()
        launch(Dispatchers.Default) {
            val playlist = playlist.value ?: return@launch
            playlistManager.delete(playlist)
        }
    }

    fun starredChipTapped() {
        lockedToFirstPage.value = false
        launch {
            playlist.value?.let { playlist ->
                playlist.starred = !playlist.starred
                playlistManager.update(playlist)
            }
        }
    }
}
