package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import kotlinx.coroutines.rx2.rxMaybe
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
    private val enabledObservable = BehaviorSubject.createDefault(false)
    private val retryCountObservable = BehaviorSubject.createDefault(0)

    fun setEnabled(enabled: Boolean) {
        if (FeatureFlag.isEnabled(Feature.RECOMMENDATIONS)) {
            enabledObservable.onNext(enabled)
        }
    }

    fun getRecommendationsFlowable(podcastUuid: String): Flowable<RecommendationsResult> {
        return Flowable
            .combineLatest(
                enabledObservable.toFlowable(BackpressureStrategy.LATEST).distinctUntilChanged(),
                retryCountObservable.toFlowable(BackpressureStrategy.LATEST).distinctUntilChanged(),
            ) { enabled, _ -> enabled }
            .switchMap { enabled ->
                if (enabled) {
                    getRecommendationsMaybe(podcastUuid)
                        .toFlowable()
                        .removePodcast(podcastUuid)
                        .addSubscribedStatusFlowable()
                        .map { listFeed ->
                            if (listFeed.podcasts.isNullOrEmpty() && listFeed.podroll.isNullOrEmpty()) {
                                RecommendationsResult.Empty
                            } else {
                                RecommendationsResult.Success(listFeed)
                            }
                        }
                        .onErrorReturn { error ->
                            Timber.e(error, "Error loading recommendations")
                            RecommendationsResult.Empty
                        }
                } else {
                    Flowable.just(RecommendationsResult.Empty)
                }
            }.startWith(RecommendationsResult.Loading)
    }

    private fun getRecommendationsMaybe(podcastUuid: String): Maybe<ListFeed> = rxMaybe {
        listRepository.getPodcastRecommendations(
            podcastUuid = podcastUuid,
            countryCode = settings.discoverCountryCode.value,
        )
    }

    private fun Flowable<ListFeed>.removePodcast(podcastUuid: String): Flowable<ListFeed> {
        return map { list ->
            val filteredPodcasts = list.podcasts?.filter { it.uuid != podcastUuid }
            list.copy(podcasts = filteredPodcasts)
        }
    }

    private fun Flowable<ListFeed>.addSubscribedStatusFlowable(): Flowable<ListFeed> {
        return switchMap { list ->
            podcastManager.getSubscribedPodcastUuidsRxSingle()
                .toFlowable()
                .mergeWith(podcastManager.podcastSubscriptionsRxFlowable())
                .map { subscribedList ->
                    val podcasts = list.podcasts?.map { podcast ->
                        podcast.copy(isSubscribed = subscribedList.contains(podcast.uuid))
                    }
                    val podroll = list.podroll?.map { podcast ->
                        podcast.copy(isSubscribed = subscribedList.contains(podcast.uuid))
                    }
                    list.copy(podcasts = podcasts, podroll = podroll)
                }
        }
    }

    fun retry() {
        retryCountObservable.onNext((retryCountObservable.value ?: 0) + 1)
    }
}
