package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import timber.log.Timber

sealed class RecommendationsResult {
    data object Loading : RecommendationsResult()
    data class Success(val listFeed: ListFeed) : RecommendationsResult()
    data object Empty : RecommendationsResult()
}

class RecommendationsHandler @Inject constructor(
    private val listRepository: ListRepository,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) {
    private val enabled = MutableStateFlow(false)
    private val retryCount = MutableStateFlow(0)

    fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getRecommendationsFlow(podcastUuid: String): Flow<RecommendationsResult> {
        // Each retry re-emits the enabled value so flatMapLatest restarts the request.
        return combine(enabled, retryCount) { enabled, _ -> enabled }
            .flatMapLatest { enabled ->
                if (enabled) {
                    recommendationsFlow(podcastUuid)
                } else {
                    flowOf(RecommendationsResult.Empty)
                }
            }
            .onStart { emit(RecommendationsResult.Loading) }
    }

    private fun recommendationsFlow(podcastUuid: String): Flow<RecommendationsResult> {
        return flow {
            // A failed request returns null and emits nothing, leaving the previous result in place.
            val listFeed = listRepository.getPodcastRecommendations(
                podcastUuid = podcastUuid,
                countryCode = settings.discoverCountryCode.value,
            ) ?: return@flow
            val feedWithoutPodcast = listFeed.copy(podcasts = listFeed.podcasts?.filter { it.uuid != podcastUuid })
            val results = podcastManager.podcastSubscriptionsFlow().map { subscribedUuids ->
                val feed = feedWithoutPodcast.withSubscribedStatus(subscribedUuids)
                if (feed.podcasts.isNullOrEmpty() && feed.podroll.isNullOrEmpty()) {
                    RecommendationsResult.Empty
                } else {
                    RecommendationsResult.Success(feed)
                }
            }
            emitAll(results)
        }.catch { error ->
            Timber.e(error, "Error loading recommendations")
            emit(RecommendationsResult.Empty)
        }
    }

    private fun ListFeed.withSubscribedStatus(subscribedUuids: List<String>): ListFeed {
        return copy(
            podcasts = podcasts?.map { podcast -> podcast.copy(isSubscribed = subscribedUuids.contains(podcast.uuid)) },
            podroll = podroll?.map { podcast -> podcast.copy(isSubscribed = subscribedUuids.contains(podcast.uuid)) },
        )
    }

    fun retry() {
        retryCount.update { it + 1 }
    }
}
