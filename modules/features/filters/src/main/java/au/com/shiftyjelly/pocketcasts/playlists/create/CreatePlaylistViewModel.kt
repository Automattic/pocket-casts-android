package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel(assistedFactory = CreatePlaylistViewModel.Factory::class)
class CreatePlaylistViewModel @AssistedInject constructor(
    private val podcastManager: PodcastManager,
    @Assisted initialPlaylistTitle: String,
) : ViewModel() {
    val playlistNameState = TextFieldState(
        initialText = initialPlaylistTitle,
        initialSelection = TextRange(0, initialPlaylistTitle.length),
    )

    private val appliedRules = MutableStateFlow(AppliedRules.Empty)
    private val rulesBuilder = MutableStateFlow(RulesBuilder.Empty)

    val uiState = combine(
        appliedRules,
        rulesBuilder,
        podcastManager.findSubscribedFlow(),
    ) { appliedRules, rulesBuilder, followedPodcasts ->
        UiState(
            appliedRules,
            rulesBuilder,
            followedPodcasts,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = UiState.Empty)

    fun applyRule(type: RuleType) {
        val rule = when (type) {
            RuleType.Podcasts -> rulesBuilder.value.podcastsRule
            RuleType.EpisodeStatus -> TODO()
            RuleType.ReleaseDate -> TODO()
            RuleType.EpisodeDuration -> TODO()
            RuleType.DownloadStatus -> TODO()
            RuleType.MediaType -> TODO()
            RuleType.Starred -> TODO()
        }
        appliedRules.update { rules ->
            rules.copy(podcasts = rule)
        }
    }

    fun useAllPodcasts(use: Boolean) {
        rulesBuilder.update { builder ->
            builder.copy(useAllPodcasts = use)
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

    data class UiState(
        val appliedRules: AppliedRules,
        val rulesBuilder: RulesBuilder,
        val followedPodcasts: List<Podcast>,
    ) {
        companion object {
            val Empty = UiState(
                appliedRules = AppliedRules.Empty,
                rulesBuilder = RulesBuilder.Empty,
                followedPodcasts = emptyList(),
            )
        }
    }

    data class AppliedRules(
        val episodeStatus: EpisodeStatusRule?,
        val downloadStatus: DownloadStatusRule?,
        val mediaType: MediaTypeRule?,
        val releaseDate: ReleaseDateRule?,
        val starred: StarredRule?,
        val podcasts: PodcastsRule?,
        val episodeDuration: EpisodeDurationRule?,
    ) {
        val isAnyRuleApplied = episodeStatus != null ||
            downloadStatus != null ||
            mediaType != null ||
            releaseDate != null ||
            starred != null ||
            podcasts != null ||
            episodeDuration != null

        companion object {
            val Empty = AppliedRules(
                episodeStatus = null,
                downloadStatus = null,
                mediaType = null,
                releaseDate = null,
                starred = null,
                podcasts = null,
                episodeDuration = null,
            )
        }
    }

    data class RulesBuilder(
        val useAllPodcasts: Boolean,
        val selectedPodcasts: Set<String>,
    ) {
        val podcastsRule get() = if (useAllPodcasts) {
            PodcastsRule.Any
        } else {
            PodcastsRule.Selected(selectedPodcasts.toList())
        }

        companion object {
            val Empty = RulesBuilder(
                useAllPodcasts = true,
                selectedPodcasts = emptySet(),
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialPlaylistName: String): CreatePlaylistViewModel
    }
}
