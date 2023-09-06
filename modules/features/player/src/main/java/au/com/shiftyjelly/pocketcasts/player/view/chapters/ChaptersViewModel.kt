package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class ChaptersViewModel
@Inject constructor(
    episodeManager: EpisodeManager,
    podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val theme: Theme
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    data class UiState(
        val chapters: List<ChapterState> = emptyList(),
        val backgroundColor: Color
    )

    data class ChapterState(
        val chapter: Chapter,
        val isPlayed: Boolean,
        val isPlaying: Boolean,
        val progress: Float = 0f
    )

    private val playbackStateObservable: Observable<PlaybackState> = playbackManager.playbackStateRelay
        .observeOn(Schedulers.io())
    private val upNextStateObservable: Observable<UpNextQueue.State> = playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
        .observeOn(Schedulers.io())

    val uiState = Observables.combineLatest(
        upNextStateObservable,
        playbackStateObservable,
        this::combineUiState
    )
        .distinctUntilChanged()
        .toFlowable(BackpressureStrategy.LATEST)

    val defaultUiState = UiState(
        chapters = emptyList(),
        backgroundColor = Color(theme.playerBackgroundColor(null))
    )

    fun skipToChapter(chapter: Chapter) {
        launch {
            playbackManager.skipToChapter(chapter)
        }
    }

    private fun combineUiState(upNextState: UpNextQueue.State, playbackState: PlaybackState): UiState {
        val podcast: Podcast? = (upNextState as? UpNextQueue.State.Loaded)?.podcast
        val backgroundColor = theme.playerBackgroundColor(podcast)

        val chapters = buildChaptersWithState(
            chapterList = playbackState.chapters.getList(),
            playbackPositionMs = playbackState.positionMs
        )
        return UiState(
            chapters = chapters,
            backgroundColor = Color(backgroundColor)
        )
    }

    private fun buildChaptersWithState(
        chapterList: List<Chapter>,
        playbackPositionMs: Int,
    ): List<ChapterState> {
        val chapters = mutableListOf<ChapterState>()
        var currentChapter: Chapter? = null
        for (chapter in chapterList) {
            val chapterState = if (currentChapter != null) {
                // a chapter that hasn't been played
                ChapterState(chapter = chapter, isPlayed = false, isPlaying = false)
            } else if (chapter.containsTime(playbackPositionMs)) {
                // the chapter currently playing
                currentChapter = chapter
                val progress = if (chapter.duration <= 0) 0f else ((playbackPositionMs - chapter.startTime) / chapter.duration.toFloat())
                ChapterState(chapter = chapter, isPlayed = false, isPlaying = true, progress = progress)
            } else {
                // a chapter that has been played
                ChapterState(chapter = chapter, isPlayed = true, isPlaying = false)
            }
            chapters.add(chapterState)
        }
        return chapters
    }
}
