package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.views.helper.ShowNotesFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class NotesViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val serverShowNotesManager: ServerShowNotesManager,
    @ApplicationContext context: Context
) : ViewModel() {

    private val showNotesFormatter = ShowNotesFormatter(context).apply {
        backgroundColor = "#FFFFFF"
        textColor = "#FFFFFF"
        linkColor = "#FFFFFF"
        setConvertTimesToLinks(true)
    }
    private val errorLoadingString = context.getString(LR.string.error_loading_show_notes)
    private var loadShowNotesJob: Job? = null

    val showNotes = MutableLiveData<ShowNotesState>().apply { postValue(ShowNotesState.Loaded("")) }
    val episode = MutableLiveData<PodcastEpisode>()

    fun loadEpisode(episode: BaseEpisode, color: Int) {
        if (episode !is PodcastEpisode || (this.episode.value?.uuid == episode.uuid && ColorUtils.colorIntToHexString(color).equals(showNotesFormatter.backgroundColor, true))) return // Only update show notes when the episode or color changes

        showNotesFormatter.backgroundColor = "#" + Integer.toHexString(color).substring(2) // Convert the color int to hex value, discard the alpha from the front
        // update the episode live data
        this.episode.postValue(episode)
        // convert times to links if the episode is now playing
        showNotesFormatter.setConvertTimesToLinks(playbackManager.upNextQueue.isCurrentEpisode(episode))
        // show the loading state
        showNotes.postValue(ShowNotesState.Loading)
        // cancel any previous jobs
        loadShowNotesJob?.cancel()
        // load the podcast and show notes in the background
        loadShowNotesJob = viewModelScope.launch {
            loadPodcastAndShowNotes(episode)
        }
    }

    private suspend fun loadPodcastAndShowNotes(episode: PodcastEpisode) {
        try {
            val podcastUuid = episode.podcastUuid
            val podcast = podcastManager.findPodcastByUuidSuspend(podcastUuid)
            if (podcast == null) {
                showNotes.postValue(ShowNotesState.NotFound)
                return
            }
            // update the show notes link tint
            val linkColor = if (podcast.tintColorForDarkBg == 0) Color.WHITE else podcast.tintColorForDarkBg
            showNotesFormatter.linkColor = ColorUtils.colorIntToHexString(linkColor)
            // load the show notes
            val state = serverShowNotesManager.loadShowNotes(podcastUuid = podcastUuid, episodeUuid = episode.uuid)
            // show an error message if the show notes couldn't be loaded
            val text = if (state is ShowNotesState.Loaded) state.showNotes else errorLoadingString
            // theme the show notes
            val formattedText = showNotesFormatter.format(text) ?: ""
            showNotes.postValue(ShowNotesState.Loaded(formattedText))
        } catch (e: Exception) {
            Timber.e(e)
            showNotes.postValue(ShowNotesState.Error(e))
        }
    }
}
