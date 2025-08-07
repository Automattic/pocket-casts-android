package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.playlists.rules.AppliedRules
import au.com.shiftyjelly.pocketcasts.playlists.rules.RuleType
import au.com.shiftyjelly.pocketcasts.playlists.rules.RulesBuilder
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CreatePlaylistViewModel.Factory::class)
class CreatePlaylistViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    @Assisted initialPlaylistTitle: String,
) : ViewModel() {
    private val _createdSmartPlaylistUuid = CompletableDeferred<String>(viewModelScope.coroutineContext[Job])

    val createdSmartPlaylistUuid: Deferred<String> = _createdSmartPlaylistUuid

    val playlistNameState = TextFieldState(
        initialText = initialPlaylistTitle,
        initialSelection = TextRange(0, initialPlaylistTitle.length),
    )

    private val appliedRules = MutableStateFlow(AppliedRules.Empty)

    private val rulesBuilder = MutableStateFlow(RulesBuilder.Empty)

    private val smartEpisodes = appliedRules.flatMapLatest { appliedRules ->
        val smartRules = appliedRules.toSmartRules()
        if (smartRules != null) {
            playlistManager.observeSmartEpisodes(smartRules)
        } else {
            flowOf(emptyList())
        }
    }

    private val totalEpisodeCount = appliedRules.flatMapLatest { appliedRules ->
        val smartRules = appliedRules.toSmartRules()
        if (smartRules != null) {
            playlistManager.observeEpisodeMetadata(smartRules).map { it.episodeCount }
        } else {
            flowOf(0)
        }
    }

    private val smartStarredEpisodes = appliedRules.flatMapLatest { appliedRules ->
        val smartRules = appliedRules.toSmartRules() ?: SmartRules.Default
        val starredRules = smartRules.copy(starred = StarredRule.Starred)
        playlistManager.observeSmartEpisodes(starredRules)
    }

    val uiState = combine(
        appliedRules,
        rulesBuilder,
        podcastManager.findSubscribedFlow(),
        smartEpisodes,
        totalEpisodeCount,
        smartStarredEpisodes,
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork(Element.Filters) },
        ::UiState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = UiState.Empty)

    fun applyRule(type: RuleType) {
        when (type) {
            RuleType.Podcasts -> {
                val rule = rulesBuilder.value.podcastsRule
                appliedRules.update { rules ->
                    rules.copy(podcasts = rule)
                }
            }

            RuleType.EpisodeStatus -> {
                val rule = rulesBuilder.value.episodeStatusRule
                appliedRules.update { rules ->
                    rules.copy(episodeStatus = rule)
                }
            }

            RuleType.ReleaseDate -> {
                val rule = rulesBuilder.value.releaseDateRule
                appliedRules.update { rules ->
                    rules.copy(releaseDate = rule)
                }
            }

            RuleType.EpisodeDuration -> {
                val rule = rulesBuilder.value.episodeDurationRule
                appliedRules.update { rules ->
                    rules.copy(episodeDuration = rule)
                }
            }

            RuleType.DownloadStatus -> {
                val rule = rulesBuilder.value.downloadStatusRule
                appliedRules.update { rules ->
                    rules.copy(downloadStatus = rule)
                }
            }

            RuleType.MediaType -> {
                val rule = rulesBuilder.value.mediaTypeRule
                appliedRules.update { rules ->
                    rules.copy(mediaType = rule)
                }
            }

            RuleType.Starred -> {
                val rule = rulesBuilder.value.starredRule
                appliedRules.update { rules ->
                    rules.copy(starred = rule)
                }
            }
        }
    }

    fun useAllPodcasts(shouldUse: Boolean) {
        rulesBuilder.update { builder ->
            builder.copy(useAllPodcasts = shouldUse)
        }
    }

    fun selectPodcast(uuid: String) {
        rulesBuilder.update { builder ->
            val podcasts = builder.selectedPodcasts + uuid
            builder.copy(selectedPodcasts = podcasts)
        }
    }

    fun deselectPodcast(uuid: String) {
        rulesBuilder.update { builder ->
            val podcasts = builder.selectedPodcasts - uuid
            builder.copy(selectedPodcasts = podcasts)
        }
    }

    fun selectAllPodcasts() {
        rulesBuilder.update { builder ->
            val podcasts = uiState.value.followedPodcasts.mapTo(mutableSetOf(), Podcast::uuid)
            builder.copy(selectedPodcasts = podcasts)
        }
    }

    fun deselectAllPodcasts() {
        rulesBuilder.update { builder ->
            builder.copy(selectedPodcasts = emptySet())
        }
    }

    fun useUnplayedEpisodes(shouldUse: Boolean) {
        rulesBuilder.update { builder ->
            val rule = builder.episodeStatusRule.copy(unplayed = shouldUse)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useInProgressEpisodes(shouldUse: Boolean) {
        rulesBuilder.update { builder ->
            val rule = builder.episodeStatusRule.copy(inProgress = shouldUse)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useCompletedEpisodes(shouldUse: Boolean) {
        rulesBuilder.update { builder ->
            val rule = builder.episodeStatusRule.copy(completed = shouldUse)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useReleaseDate(rule: ReleaseDateRule) {
        rulesBuilder.update { builder ->
            builder.copy(releaseDateRule = rule)
        }
    }

    fun useConstrainedDuration(shouldUse: Boolean) {
        rulesBuilder.update { builder ->
            builder.copy(isEpisodeDurationConstrained = shouldUse)
        }
    }

    fun decrementMinDuration() {
        rulesBuilder.update { builder -> builder.decrementMinDuration() }
    }

    fun incrementMinDuration() {
        rulesBuilder.update { builder -> builder.incrementMinDuration() }
    }

    fun decrementMaxDuration() {
        rulesBuilder.update { builder -> builder.decrementMaxDuration() }
    }

    fun incrementMaxDuration() {
        rulesBuilder.update { builder -> builder.incrementMaxDuration() }
    }

    fun useDownloadStatus(rule: DownloadStatusRule) {
        rulesBuilder.update { builder ->
            builder.copy(downloadStatusRule = rule)
        }
    }

    fun useMediaType(rule: MediaTypeRule) {
        rulesBuilder.update { builder ->
            builder.copy(mediaTypeRule = rule)
        }
    }

    fun useStarredEpisodes(shouldUse: Boolean) {
        rulesBuilder.update { builder ->
            builder.copy(useStarredEpisode = shouldUse)
        }
    }

    private var isCreationTriggered = false

    fun createSmartPlaylist() {
        val rules = appliedRules.value.toSmartRules() ?: SmartRules.Default
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
