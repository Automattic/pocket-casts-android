package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.playlists.edit.SmartRulesEditor
import au.com.shiftyjelly.pocketcasts.playlists.rules.AppliedRules
import au.com.shiftyjelly.pocketcasts.playlists.rules.RuleType
import au.com.shiftyjelly.pocketcasts.playlists.rules.RulesBuilder
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = CreatePlaylistViewModel.Factory::class)
class CreatePlaylistViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    rulesEditorFactory: SmartRulesEditor.Factory,
    settings: Settings,
    @Assisted initialPlaylistTitle: String,
) : ViewModel() {
    private val _createdSmartPlaylistUuid = CompletableDeferred<String>(viewModelScope.coroutineContext[Job])

    val createdSmartPlaylistUuid: Deferred<String> = _createdSmartPlaylistUuid

    val playlistNameState = TextFieldState(
        initialText = initialPlaylistTitle,
        initialSelection = TextRange(0, initialPlaylistTitle.length),
    )

    private val rulesEditor = rulesEditorFactory.create(
        scope = viewModelScope,
        initialBuilder = RulesBuilder.Empty,
        initialAppliedRules = AppliedRules.Empty,
    )

    val uiState = combine(
        rulesEditor.rulesFlow,
        rulesEditor.builderFlow,
        rulesEditor.followedPodcasts,
        rulesEditor.smartEpisodes,
        rulesEditor.totalEpisodeCount,
        rulesEditor.smartStarredEpisodes,
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork(Element.Filters) },
        ::UiState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = UiState.Empty)

    fun applyRule(type: RuleType) {
        rulesEditor.applyRule(type)
    }

    fun useAllPodcasts(shouldUse: Boolean) {
        rulesEditor.useAllPodcasts(shouldUse)
    }

    fun selectPodcast(uuid: String) {
        rulesEditor.selectPodcast(uuid)
    }

    fun deselectPodcast(uuid: String) {
        rulesEditor.deselectPodcast(uuid)
    }

    fun selectAllPodcasts() {
        rulesEditor.selectAllPodcasts()
    }

    fun deselectAllPodcasts() {
        rulesEditor.deselectAllPodcasts()
    }

    fun useUnplayedEpisodes(shouldUse: Boolean) {
        rulesEditor.useUnplayedEpisodes(shouldUse)
    }

    fun useInProgressEpisodes(shouldUse: Boolean) {
        rulesEditor.useInProgressEpisodes(shouldUse)
    }

    fun useCompletedEpisodes(shouldUse: Boolean) {
        rulesEditor.useCompletedEpisodes(shouldUse)
    }

    fun useReleaseDate(rule: ReleaseDateRule) {
        rulesEditor.useReleaseDate(rule)
    }

    fun useConstrainedDuration(shouldUse: Boolean) {
        rulesEditor.useConstrainedDuration(shouldUse)
    }

    fun decrementMinDuration() {
        rulesEditor.decrementMinDuration()
    }

    fun incrementMinDuration() {
        rulesEditor.incrementMinDuration()
    }

    fun decrementMaxDuration() {
        rulesEditor.decrementMaxDuration()
    }

    fun incrementMaxDuration() {
        rulesEditor.incrementMaxDuration()
    }

    fun useDownloadStatus(rule: DownloadStatusRule) {
        rulesEditor.useDownloadStatus(rule)
    }

    fun useMediaType(rule: MediaTypeRule) {
        rulesEditor.useMediaType(rule)
    }

    fun useStarredEpisodes(shouldUse: Boolean) {
        rulesEditor.useStarredEpisodes(shouldUse)
    }

    fun clearTransientRules() {
        rulesEditor.clearTransientRules()
    }

    private var isCreationTriggered = false

    fun createSmartPlaylist() {
        val rules = rulesEditor.rulesFlow.value.toSmartRulesOrDefault()
        if (isCreationTriggered) {
            return
        }
        isCreationTriggered = true

        val draft = SmartPlaylistDraft(
            title = playlistNameState.text.toString(),
            rules = rules,
        )
        viewModelScope.launch {
            val playlistUuid = playlistManager.upsertSmartPlaylist(draft)
            _createdSmartPlaylistUuid.complete(playlistUuid)
        }
    }

    data class UiState(
        val appliedRules: AppliedRules,
        val rulesBuilder: RulesBuilder,
        val followedPodcasts: List<Podcast>,
        val smartEpisodes: List<PodcastEpisode>,
        val totalEpisodeCount: Int,
        val smartStarredEpisodes: List<PodcastEpisode>,
        val useEpisodeArtwork: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                appliedRules = AppliedRules.Empty,
                rulesBuilder = RulesBuilder.Empty,
                followedPodcasts = emptyList(),
                totalEpisodeCount = 0,
                smartEpisodes = emptyList(),
                smartStarredEpisodes = emptyList(),
                useEpisodeArtwork = false,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialPlaylistName: String): CreatePlaylistViewModel
    }
}
