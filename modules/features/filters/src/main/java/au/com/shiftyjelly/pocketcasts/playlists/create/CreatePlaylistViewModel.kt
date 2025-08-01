package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CreatePlaylistViewModel.Factory::class)
class CreatePlaylistViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    @Assisted initialPlaylistTitle: String,
) : ViewModel() {
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

    val uiState = combine(
        appliedRules,
        rulesBuilder,
        podcastManager.findSubscribedFlow(),
        smartEpisodes,
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

            RuleType.Starred -> TODO()
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

    fun useUnplayedEpisodes(use: Boolean) {
        rulesBuilder.update { builder ->
            val rule = builder.episodeStatusRule.copy(unplayed = use)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useInProgressEpisodes(use: Boolean) {
        rulesBuilder.update { builder ->
            val rule = builder.episodeStatusRule.copy(inProgress = use)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useCompletedEpisodes(use: Boolean) {
        rulesBuilder.update { builder ->
            val rule = builder.episodeStatusRule.copy(completed = use)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useReleaseDate(rule: ReleaseDateRule) {
        rulesBuilder.update { builder ->
            builder.copy(releaseDateRule = rule)
        }
    }

    fun constrainDuration(isConstrained: Boolean) {
        rulesBuilder.update { builder ->
            builder.copy(isEpisodeDurationConstrained = isConstrained)
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

    data class UiState(
        val appliedRules: AppliedRules,
        val rulesBuilder: RulesBuilder,
        val followedPodcasts: List<Podcast>,
        val smartEpisodes: List<PodcastEpisode>,
        val useEpisodeArtwork: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                appliedRules = AppliedRules.Empty,
                rulesBuilder = RulesBuilder.Empty,
                followedPodcasts = emptyList(),
                smartEpisodes = emptyList(),
                useEpisodeArtwork = false,
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

        fun toSmartRules() = if (isAnyRuleApplied) {
            SmartRules(
                episodeStatus = episodeStatus ?: SmartRules.Default.episodeStatus,
                downloadStatus = downloadStatus ?: SmartRules.Default.downloadStatus,
                mediaType = mediaType ?: SmartRules.Default.mediaType,
                releaseDate = releaseDate ?: SmartRules.Default.releaseDate,
                starred = starred ?: SmartRules.Default.starred,
                podcasts = podcasts ?: SmartRules.Default.podcasts,
                episodeDuration = episodeDuration ?: SmartRules.Default.episodeDuration,
            )
        } else {
            null
        }

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
        val episodeStatusRule: EpisodeStatusRule,
        val releaseDateRule: ReleaseDateRule,
        val isEpisodeDurationConstrained: Boolean,
        val minEpisodeDuration: Duration,
        val maxEpisodeDuration: Duration,
        val downloadStatusRule: DownloadStatusRule,
        val mediaTypeRule: MediaTypeRule,
    ) {
        val podcastsRule
            get() = if (useAllPodcasts) {
                PodcastsRule.Any
            } else {
                PodcastsRule.Selected(selectedPodcasts.toList())
            }

        fun decrementMinDuration(): RulesBuilder {
            val minDuration = minEpisodeDuration
            val newDuration = minDuration - if (minDuration > 5.minutes) 5.minutes else 1.minutes
            return if (newDuration >= ZERO && newDuration < maxEpisodeDuration) {
                copy(minEpisodeDuration = newDuration)
            } else {
                this
            }
        }

        fun incrementMinDuration(): RulesBuilder {
            val minDuration = minEpisodeDuration
            val newDuration = minDuration + if (minDuration >= 5.minutes) 5.minutes else 1.minutes
            return if (newDuration >= ZERO && newDuration < maxEpisodeDuration) {
                copy(minEpisodeDuration = newDuration)
            } else {
                this
            }
        }

        fun decrementMaxDuration(): RulesBuilder {
            val maxDuration = maxEpisodeDuration
            val newDuration = maxDuration - if (maxDuration > 5.minutes) 5.minutes else 1.minutes
            return if (newDuration > ZERO && newDuration > minEpisodeDuration) {
                copy(maxEpisodeDuration = newDuration)
            } else {
                this
            }
        }

        fun incrementMaxDuration(): RulesBuilder {
            val maxDuration = maxEpisodeDuration
            val newDuration = maxDuration + if (maxDuration >= 5.minutes) 5.minutes else 1.minutes
            return if (newDuration > ZERO && newDuration > minEpisodeDuration) {
                copy(maxEpisodeDuration = newDuration)
            } else {
                this
            }
        }

        val episodeDurationRule
            get() = if (isEpisodeDurationConstrained) {
                EpisodeDurationRule.Constrained(minEpisodeDuration, maxEpisodeDuration)
            } else {
                EpisodeDurationRule.Any
            }

        companion object {
            val Empty = RulesBuilder(
                useAllPodcasts = true,
                selectedPodcasts = emptySet(),
                episodeStatusRule = SmartRules.Default.episodeStatus,
                releaseDateRule = SmartRules.Default.releaseDate,
                isEpisodeDurationConstrained = false,
                minEpisodeDuration = 20.minutes,
                maxEpisodeDuration = 40.minutes,
                downloadStatusRule = SmartRules.Default.downloadStatus,
                mediaTypeRule = SmartRules.MediaTypeRule.Any,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialPlaylistName: String): CreatePlaylistViewModel
    }
}
