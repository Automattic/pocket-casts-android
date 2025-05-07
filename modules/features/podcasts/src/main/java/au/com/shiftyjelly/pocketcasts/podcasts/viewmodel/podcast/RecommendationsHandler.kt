package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
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
    data class Success(val listFeed: ListFeed) : RecommendationsResult()
    data object Empty : RecommendationsResult()
}

class RecommendationsHandler @Inject constructor(
    private val listRepository: ListRepository,
    private val podcastManager: PodcastManager,
) {
    private val enabledObservable = BehaviorSubject.createDefault(false)

    fun setEnabled(enabled: Boolean) {
        if (FeatureFlag.isEnabled(Feature.RECOMMENDATIONS)) {
            enabledObservable.onNext(enabled)
        }
    }

    private fun isEnabledFlowable(): Flowable<Boolean> {
        return enabledObservable.toFlowable(BackpressureStrategy.LATEST)
    }

    fun getRecommendationsFlowable(podcast: Podcast): Flowable<RecommendationsResult> {
        return isEnabledFlowable()
            .distinctUntilChanged()
            .switchMap { enabled ->
                if (enabled) {
                    getRecommendationsMaybe(podcast)
                        .toFlowable()
                        .addSubscribedStatusFlowable()
                        .map { listFeed ->
                            if (listFeed.podcasts.isNullOrEmpty()) {
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
            }
    }

    private fun getRecommendationsMaybe(podcast: Podcast): Maybe<ListFeed> = rxMaybe {
        listRepository.getPodcastRecommendations(podcast.uuid)
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
}
