package au.com.shiftyjelly.pocketcasts.playlists.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.playlists.rules.AppliedRules
import au.com.shiftyjelly.pocketcasts.playlists.rules.RuleType
import au.com.shiftyjelly.pocketcasts.playlists.rules.RulesBuilder
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SmartRulesEditViewModel.Factory::class)
class SmartRulesEditViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    rulesEditorFactory: SmartRulesEditor.Factory,
    settings: Settings,
    @Assisted private val playlistUuid: String,
) : ViewModel() {
    private var rulesEditor: SmartRulesEditor? = null

    val uiState = flow {
        val playlist = playlistManager.observeSmartPlaylist(playlistUuid).first()
        if (playlist != null) {
            val smartRules = playlist.smartRules
            val editor = rulesEditorFactory.create(
                scope = viewModelScope,
                initialBuilder = RulesBuilder.Empty.applyRules(smartRules),
                initialAppliedRules = AppliedRules(
                    episodeStatus = smartRules.episodeStatus,
                    downloadStatus = smartRules.downloadStatus,
                    mediaType = smartRules.mediaType,
                    releaseDate = smartRules.releaseDate,
                    starred = smartRules.starred,
                    podcasts = smartRules.podcasts,
                    episodeDuration = smartRules.episodeDuration,
                ),
            )
            rulesEditor = editor
            // Add a small delay to prevent rendering all data while the bottom sheet is still animating in
            delay(300)
            emitAll(
                combine(
                    editor.rulesFlow,
                    editor.builderFlow,
                    editor.followedPodcasts,
                    editor.smartEpisodes,
                    editor.totalEpisodeCount,
                    editor.smartStarredEpisodes,
                    settings.artworkConfiguration.flow.map { it.useEpisodeArtwork(Element.Filters) },
                ) { rules, builder, podcasts, episodes, episodeCount, smartEpisodes, showEpisodeArtwork ->
                    UiState(
                        playlistTitle = playlist.title,
                        appliedRules = rules,
                        rulesBuilder = builder,
                        followedPodcasts = podcasts,
                        smartEpisodes = episodes,
                        totalEpisodeCount = episodeCount,
                        smartStarredEpisodes = smartEpisodes,
                        useEpisodeArtwork = showEpisodeArtwork,
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    fun applyRule(type: RuleType) {
        rulesEditor?.applyRule(type)
        viewModelScope.launch(NonCancellable) {
            val smartRules = uiState.value?.appliedRules?.toSmartRules()
            if (smartRules != null) {
                playlistManager.updateSmartRules(playlistUuid, smartRules)
            }
        }
    }

    fun useAllPodcasts(shouldUse: Boolean) {
        rulesEditor?.useAllPodcasts(shouldUse)
    }

    fun selectPodcast(uuid: String) {
        rulesEditor?.selectPodcast(uuid)
    }

    fun deselectPodcast(uuid: String) {
        rulesEditor?.deselectPodcast(uuid)
    }

    fun selectAllPodcasts() {
        rulesEditor?.selectAllPodcasts()
    }

    fun deselectAllPodcasts() {
        rulesEditor?.deselectAllPodcasts()
    }

    fun useUnplayedEpisodes(shouldUse: Boolean) {
        rulesEditor?.useUnplayedEpisodes(shouldUse)
    }

    fun useInProgressEpisodes(shouldUse: Boolean) {
        rulesEditor?.useInProgressEpisodes(shouldUse)
    }

    fun useCompletedEpisodes(shouldUse: Boolean) {
        rulesEditor?.useCompletedEpisodes(shouldUse)
    }

    fun useReleaseDate(rule: ReleaseDateRule) {
        rulesEditor?.useReleaseDate(rule)
    }

    fun useConstrainedDuration(shouldUse: Boolean) {
        rulesEditor?.useConstrainedDuration(shouldUse)
    }

    fun decrementMinDuration() {
        rulesEditor?.decrementMinDuration()
    }

    fun incrementMinDuration() {
        rulesEditor?.incrementMinDuration()
    }

    fun decrementMaxDuration() {
        rulesEditor?.decrementMaxDuration()
    }

    fun incrementMaxDuration() {
        rulesEditor?.incrementMaxDuration()
    }

    fun useDownloadStatus(rule: DownloadStatusRule) {
        rulesEditor?.useDownloadStatus(rule)
    }

    fun useMediaType(rule: MediaTypeRule) {
        rulesEditor?.useMediaType(rule)
    }

    fun useStarredEpisodes(shouldUse: Boolean) {
        rulesEditor?.useStarredEpisodes(shouldUse)
    }

    fun clearTransientRules() {
        rulesEditor?.clearTransientRules()
    }

    data class UiState(
        val playlistTitle: String,
        val appliedRules: AppliedRules,
        val rulesBuilder: RulesBuilder,
        val followedPodcasts: List<Podcast>,
        val smartEpisodes: List<PodcastEpisode>,
        val totalEpisodeCount: Int,
        val smartStarredEpisodes: List<PodcastEpisode>,
        val useEpisodeArtwork: Boolean,
    )

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): SmartRulesEditViewModel
    }
}
