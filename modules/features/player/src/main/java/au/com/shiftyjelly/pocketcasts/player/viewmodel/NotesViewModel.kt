package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.CachedServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotesViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val serverShowNotesManager: ServerShowNotesManager,
    settings: Settings,
    @ApplicationContext context: Context
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val showNotesFormatter = ShowNotesFormatter(settings, context).apply {
        backgroundColor = "#FFFFFF"
        textColor = "#FFFFFF"
        linkColor = "#FFFFFF"
        setConvertTimesToLinks(true)
    }

    val showNotes = MutableLiveData<Pair<String, Boolean>>().apply { postValue(Pair("", false)) }
    val episode = MutableLiveData<Episode>()

    fun loadEpisode(playable: Playable, color: Int) {
        if (playable !is Episode || (episode.value?.uuid == playable.uuid && ColorUtils.colorIntToHexString(color).equals(showNotesFormatter.backgroundColor, true))) return // Only update show notes when the episode or color changes

        showNotesFormatter.backgroundColor = "#" + Integer.toHexString(color).substring(2) // Convert the color int to hex value, discard the alpha from the front
        // update the episode live data
        episode.postValue(playable)
        // convert times to links if the episode is now playing
        showNotesFormatter.setConvertTimesToLinks(playbackManager.upNextQueue.isCurrentEpisode(playable))
        podcastManager.findPodcastByUuidRx(playable.podcastUuid)
            // update the show notes link tint
            .doOnSuccess { podcast ->
                val linkColor = if (podcast.tintColorForDarkBg == 0) Color.WHITE else podcast.tintColorForDarkBg
                showNotesFormatter.linkColor = ColorUtils.colorIntToHexString(linkColor)
            }
            // load the show notes
            .flatMapCompletable { loadShowNotes(playable.uuid) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    private fun loadShowNotes(episodeUuid: String): Completable {
        // Clear previous show notes while loading new notes
        updateShowNotes("", inProgress = true)

        return Completable.fromAction {
            serverShowNotesManager.loadShowNotes(
                episodeUuid,
                object : CachedServerCallback<String> {
                    override fun cachedDataFound(data: String) {
                        updateShowNotes(data, inProgress = false)
                    }

                    override fun networkDataFound(data: String) {
                        updateShowNotes(data, inProgress = false)
                    }

                    override fun notFound() {
                        updateShowNotes("", inProgress = false)
                    }
                }
            )
        }
    }

    private fun updateShowNotes(notes: String, inProgress: Boolean) {
        showNotesFormatter.format(notes)?.let { formatted ->
            showNotes.postValue(Pair(formatted, inProgress))
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
