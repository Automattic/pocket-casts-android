package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.views.helper.ShowNotesFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.rxCompletable
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class NotesViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val serverShowNotesManager: ServerShowNotesManager,
    @ApplicationContext context: Context
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val showNotesFormatter = ShowNotesFormatter(context).apply {
        backgroundColor = "#FFFFFF"
        textColor = "#FFFFFF"
        linkColor = "#FFFFFF"
        setConvertTimesToLinks(true)
    }
    private val errorLoadingString = context.getString(LR.string.error_loading_show_notes)

    val showNotes = MutableLiveData<ShowNotesState>().apply { postValue(ShowNotesState.Loaded("")) }
    val episode = MutableLiveData<PodcastEpisode>()

    fun loadEpisode(episode: BaseEpisode, color: Int) {
        if (episode !is PodcastEpisode || (this.episode.value?.uuid == episode.uuid && ColorUtils.colorIntToHexString(color).equals(showNotesFormatter.backgroundColor, true))) return // Only update show notes when the episode or color changes

        showNotesFormatter.backgroundColor = "#" + Integer.toHexString(color).substring(2) // Convert the color int to hex value, discard the alpha from the front
        // update the episode live data
        this.episode.postValue(episode)
        // convert times to links if the episode is now playing
        showNotesFormatter.setConvertTimesToLinks(playbackManager.upNextQueue.isCurrentEpisode(episode))
        podcastManager.findPodcastByUuidRx(episode.podcastUuid)
            // update the show notes link tint
            .doOnSuccess { podcast ->
                val linkColor = if (podcast.tintColorForDarkBg == 0) Color.WHITE else podcast.tintColorForDarkBg
                showNotesFormatter.linkColor = ColorUtils.colorIntToHexString(linkColor)
            }
            // load the show notes
            .flatMapCompletable { loadShowNotes(podcastUuid = episode.podcastUuid, episodeUuid = episode.uuid) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    private fun loadShowNotes(podcastUuid: String, episodeUuid: String): Completable {
        // Clear previous show notes while loading new notes
        showNotes.postValue(ShowNotesState.Loading)
        return rxCompletable {
            val state = serverShowNotesManager.loadShowNotes(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
            val text = if (state is ShowNotesState.Loaded) state.showNotes else errorLoadingString
            val formattedText = showNotesFormatter.format(text) ?: ""
            showNotes.postValue(ShowNotesState.Loaded(formattedText))
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
