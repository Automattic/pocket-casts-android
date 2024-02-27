package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class ChaptersViewModel
@Inject constructor(
    episodeManager: EpisodeManager,
    podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val theme: Theme,
    private val settings: Settings,
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    data class UiState(
        val allChapters: List<ChapterState> = emptyList(),
        val displayChapters: List<ChapterState> = emptyList(),
        val totalChaptersCount: Int = 0,
        val backgroundColor: Color,
        val isTogglingChapters: Boolean = false,
        val userTier: UserTier = UserTier.Free,
        val canSkipChapters: Boolean = false,
        val podcast: Podcast? = null,
    ) {
        val showSubscriptionIcon
            get() = !isTogglingChapters && !canSkipChapters
    }

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
        UiState(
            backgroundColor = Color(theme.playerBackgroundColor(null)),
            userTier = settings.userTier,
            canSkipChapters = canSkipChapters(settings.userTier),
        ),
    )
    val uiState: StateFlow<UiState>
        get() = _uiState

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    private val _snackbarMessage: MutableSharedFlow<Int> = MutableSharedFlow()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                upNextStateObservable.asFlow(),
                playbackStateObservable.asFlow(),
                settings.cachedSubscriptionStatus.flow,
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
        cachedSubscriptionStatus: SubscriptionStatus?,
    ): UiState {
        val podcast: Podcast? = (upNextState as? UpNextQueue.State.Loaded)?.podcast
        val backgroundColor = theme.playerBackgroundColor(podcast)

        val chapters = buildChaptersWithState(
            chapters = playbackState.chapters,
            playbackPositionMs = playbackState.positionMs,
            lastChangeFrom = playbackState.lastChangeFrom,
        )
        val currentUserTier = (cachedSubscriptionStatus as? SubscriptionStatus.Paid)?.tier?.toUserTier() ?: UserTier.Free
        val lastUserTier = _uiState.value.userTier
        val canSkipChapters = canSkipChapters(currentUserTier)
        val isTogglingChapters = ((lastUserTier != currentUserTier) && canSkipChapters) || _uiState.value.isTogglingChapters

        return UiState(
            allChapters = chapters,
            displayChapters = getFilteredChaptersIfNeeded(
                chapters = chapters,
                isTogglingChapters = isTogglingChapters,
                userTier = currentUserTier,
            ),
            totalChaptersCount = chapters.size,
            backgroundColor = Color(backgroundColor),
            isTogglingChapters = isTogglingChapters,
            userTier = currentUserTier,
            canSkipChapters = canSkipChapters,
            podcast = playbackState.podcast,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun buildChaptersWithState(
        chapters: Chapters,
        playbackPositionMs: Int,
        lastChangeFrom: String? = null,
    ): List<ChapterState> {
        val chapterStates = mutableListOf<ChapterState>()
        var currentChapter: Chapter? = null
        for (chapter in chapters.getList()) {
            val chapterState = if (currentChapter != null) {
                // a chapter that hasn't been played
                ChapterState.NotPlayed(chapter)
            } else if (chapter.containsTime(playbackPositionMs)) {
                if (chapter.selected || !FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) {
                    // the chapter currently playing
                    currentChapter = chapter
                    val progress = chapter.calculateProgress(playbackPositionMs)
                    ChapterState.Playing(chapter = chapter, progress = progress)
                } else {
                    if (!listOf(
                            PlaybackManager.LastChangeFrom.OnUserSeeking.value,
                            PlaybackManager.LastChangeFrom.OnSeekComplete.value,
                        ).contains(lastChangeFrom)
                    ) {
                        playbackManager.skipToNextSelectedOrLastChapter()
                    }
                    ChapterState.NotPlayed(chapter)
                }
            } else {
                // a chapter that has been played
                ChapterState.Played(chapter)
            }
            chapterStates.add(chapterState)
        }
        return chapterStates
    }

    fun onSelectionChange(selected: Boolean, chapter: Chapter) {
        val selectedChapters = _uiState.value.allChapters.filter { it.chapter.selected }
        if (!selected && selectedChapters.size == 1) {
            viewModelScope.launch {
                _snackbarMessage.emit(LR.string.select_one_chapter_message)
            }
        } else {
            playbackManager.toggleChapter(selected, chapter)
        }
    }

    fun onSkipChaptersClick(checked: Boolean) {
        if (_uiState.value.canSkipChapters) {
            _uiState.value = _uiState.value.copy(
                isTogglingChapters = checked,
                displayChapters = getFilteredChaptersIfNeeded(
                    chapters = _uiState.value.allChapters,
                    isTogglingChapters = checked,
                    userTier = _uiState.value.userTier,
                ),
            )
        } else {
            viewModelScope.launch {
                _navigationState.emit(NavigationState.StartUpsell)
            }
        }
    }

    private fun getFilteredChaptersIfNeeded(
        chapters: List<ChapterState>,
        isTogglingChapters: Boolean,
        userTier: UserTier,
    ): List<ChapterState> {
        val shouldFilterChapters = canSkipChapters(userTier) &&
            !isTogglingChapters

        return if (shouldFilterChapters) {
            chapters.filter { it.chapter.selected }
        } else {
            chapters
        }
    }

    private fun canSkipChapters(userTier: UserTier) = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
        Feature.isUserEntitled(Feature.DESELECT_CHAPTERS, userTier)

    sealed class NavigationState {
        data object StartUpsell : NavigationState()
    }
}
