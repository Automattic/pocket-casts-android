package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class ChaptersViewModel
@Inject constructor(
    episodeManager: EpisodeManager,
    podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val theme: Theme,
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    data class UiState(
        val chapters: List<ChapterState> = emptyList(),
        val backgroundColor: Color,
    )

    sealed class ChapterState {
        abstract val chapter: Chapter

        data class Played(override val chapter: Chapter) : ChapterState()
        data class Playing(val progress: Float, override val chapter: Chapter) : ChapterState()
        data class NotPlayed(override val chapter: Chapter) : ChapterState()
    }

    private val _scrollToChapterState = MutableStateFlow<Chapter?>(null)
    val scrollToChapterState = _scrollToChapterState.asStateFlow()

    fun setScrollToChapter(chapter: Chapter?) {
        _scrollToChapterState.value = chapter
    }

    private val playbackStateObservable: Observable<PlaybackState> = playbackManager.playbackStateRelay
        .observeOn(Schedulers.io())
    private val upNextStateObservable: Observable<UpNextQueue.State> = playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
        .observeOn(Schedulers.io())

    private val _uiState = MutableStateFlow(
        UiState(backgroundColor = Color(theme.playerBackgroundColor(null))),
    )
    val uiState: StateFlow<UiState>
        get() = _uiState

    init {
        viewModelScope.launch {
            combine(
                upNextStateObservable.asFlow(),
                playbackStateObservable.asFlow(),
                this@ChaptersViewModel::combineUiState,
            )
                .distinctUntilChanged()
                .stateIn(viewModelScope)
                .collectLatest {
                    _uiState.value = it
                }
        }
    }

    fun skipToChapter(chapter: Chapter) {
        launch {
            playbackManager.skipToChapter(chapter)
        }
    }

    private fun combineUiState(
        upNextState: UpNextQueue.State,
        playbackState: PlaybackState,
    ): UiState {
        val podcast: Podcast? = (upNextState as? UpNextQueue.State.Loaded)?.podcast
        val backgroundColor = theme.playerBackgroundColor(podcast)

        val chapters = buildChaptersWithState(
            chapterList = playbackState.chapters.getList(),
            playbackPositionMs = playbackState.positionMs,
        )
        return UiState(
            chapters = chapters,
            backgroundColor = Color(backgroundColor),
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
                ChapterState.NotPlayed(chapter)
            } else if (chapter.containsTime(playbackPositionMs)) {
                // the chapter currently playing
                currentChapter = chapter
                val progress = chapter.calculateProgress(playbackPositionMs)
                ChapterState.Playing(chapter = chapter, progress = progress)
            } else {
                // a chapter that has been played
                ChapterState.Played(chapter)
            }
            chapters.add(chapterState)
        }
        return chapters
    }

    fun onSelectionChange(selected: Boolean, chapter: Chapter) {
        playbackManager.toggleChapter(selected, chapter)
    }
}
