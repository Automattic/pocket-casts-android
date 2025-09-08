package au.com.shiftyjelly.pocketcasts.playlists.smart

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class SmartRulesEditor @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    @Assisted scope: CoroutineScope,
    @Assisted initialBuilder: RulesBuilder,
    @Assisted initialAppliedRules: AppliedRules,
) {
    private val _builderFlow = MutableStateFlow(initialBuilder)
    val builderFlow = _builderFlow.asStateFlow()

    private val _rulesFlow = MutableStateFlow(initialAppliedRules)
    val rulesFlow = _rulesFlow.asStateFlow()

    val smartEpisodes = rulesFlow.flatMapLatest { appliedRules ->
        val smartRules = appliedRules.toSmartRules()
        if (smartRules != null) {
            playlistManager.smartEpisodesFlow(smartRules)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(scope, SharingStarted.Companion.Lazily, initialValue = emptyList())

    val totalEpisodeCount = rulesFlow.flatMapLatest { appliedRules ->
        val smartRules = appliedRules.toSmartRules() ?: SmartRules.Companion.Default
        val starredRules = smartRules.copy(starred = SmartRules.StarredRule.Starred)
        playlistManager.smartEpisodesMetadataFlow(starredRules).map { it.episodeCount }
    }.stateIn(scope, SharingStarted.Companion.Lazily, initialValue = 0)

    val smartStarredEpisodes = rulesFlow.flatMapLatest { appliedRules ->
        val smartRules = appliedRules.toSmartRules() ?: SmartRules.Companion.Default
        val starredRules = smartRules.copy(starred = SmartRules.StarredRule.Starred)
        playlistManager.smartEpisodesFlow(starredRules)
    }.stateIn(scope, SharingStarted.Companion.Lazily, initialValue = emptyList())

    val followedPodcasts = podcastManager
        .findSubscribedFlow()
        .stateIn(scope, SharingStarted.Companion.Lazily, initialValue = emptyList())

    fun useAllPodcasts(shouldUse: Boolean) {
        _builderFlow.update { builder ->
            builder.copy(useAllPodcasts = shouldUse)
        }
    }

    fun selectPodcast(uuid: String) {
        _builderFlow.update { builder ->
            val podcasts = builder.selectedPodcasts + uuid
            builder.copy(selectedPodcasts = podcasts)
        }
    }

    fun deselectPodcast(uuid: String) {
        _builderFlow.update { builder ->
            val podcasts = builder.selectedPodcasts - uuid
            builder.copy(selectedPodcasts = podcasts)
        }
    }

    fun selectAllPodcasts() {
        _builderFlow.update { builder ->
            val uuids = followedPodcasts.value.mapTo(mutableSetOf(), Podcast::uuid)
            builder.copy(selectedPodcasts = uuids)
        }
    }

    fun deselectAllPodcasts() {
        _builderFlow.update { builder ->
            builder.copy(selectedPodcasts = emptySet())
        }
    }

    fun useUnplayedEpisodes(shouldUse: Boolean) {
        _builderFlow.update { builder ->
            val rule = builder.episodeStatusRule.copy(unplayed = shouldUse)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useInProgressEpisodes(shouldUse: Boolean) {
        _builderFlow.update { builder ->
            val rule = builder.episodeStatusRule.copy(inProgress = shouldUse)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useCompletedEpisodes(shouldUse: Boolean) {
        _builderFlow.update { builder ->
            val rule = builder.episodeStatusRule.copy(completed = shouldUse)
            builder.copy(episodeStatusRule = rule)
        }
    }

    fun useReleaseDate(rule: SmartRules.ReleaseDateRule) {
        _builderFlow.update { builder ->
            builder.copy(releaseDateRule = rule)
        }
    }

    fun useConstrainedDuration(shouldUse: Boolean) {
        _builderFlow.update { builder ->
            builder.copy(isEpisodeDurationConstrained = shouldUse)
        }
    }

    fun decrementMinDuration() {
        _builderFlow.update { builder -> builder.decrementMinDuration() }
    }

    fun incrementMinDuration() {
        _builderFlow.update { builder -> builder.incrementMinDuration() }
    }

    fun decrementMaxDuration() {
        _builderFlow.update { builder -> builder.decrementMaxDuration() }
    }

    fun incrementMaxDuration() {
        _builderFlow.update { builder -> builder.incrementMaxDuration() }
    }

    fun useDownloadStatus(rule: SmartRules.DownloadStatusRule) {
        _builderFlow.update { builder ->
            builder.copy(downloadStatusRule = rule)
        }
    }

    fun useMediaType(rule: SmartRules.MediaTypeRule) {
        _builderFlow.update { builder ->
            builder.copy(mediaTypeRule = rule)
        }
    }

    fun useStarredEpisodes(shouldUse: Boolean) {
        _builderFlow.update { builder ->
            builder.copy(useStarredEpisode = shouldUse)
        }
    }

    fun applyRule(type: RuleType) {
        when (type) {
            RuleType.Podcasts -> {
                val rule = _builderFlow.value.podcastsRule
                _rulesFlow.update { rules ->
                    rules.copy(podcasts = rule)
                }
            }

            RuleType.EpisodeStatus -> {
                val rule = _builderFlow.value.episodeStatusRule
                _rulesFlow.update { rules ->
                    rules.copy(episodeStatus = rule)
                }
            }

            RuleType.ReleaseDate -> {
                val rule = _builderFlow.value.releaseDateRule
                _rulesFlow.update { rules ->
                    rules.copy(releaseDate = rule)
                }
            }

            RuleType.EpisodeDuration -> {
                val rule = _builderFlow.value.episodeDurationRule
                _rulesFlow.update { rules ->
                    rules.copy(episodeDuration = rule)
                }
            }

            RuleType.DownloadStatus -> {
                val rule = _builderFlow.value.downloadStatusRule
                _rulesFlow.update { rules ->
                    rules.copy(downloadStatus = rule)
                }
            }

            RuleType.MediaType -> {
                val rule = _builderFlow.value.mediaTypeRule
                _rulesFlow.update { rules ->
                    rules.copy(mediaType = rule)
                }
            }

            RuleType.Starred -> {
                val rule = _builderFlow.value.starredRule
                _rulesFlow.update { rules ->
                    rules.copy(starred = rule)
                }
            }
        }
    }

    fun clearTransientRules() {
        val appliedRules = _rulesFlow.value
        _builderFlow.update { builder -> builder.applyRules(appliedRules.toSmartRulesOrDefault()) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            scope: CoroutineScope,
            initialBuilder: RulesBuilder,
            initialAppliedRules: AppliedRules,
        ): SmartRulesEditor
    }
}
