package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import kotlinx.coroutines.rx2.rxSingle
import timber.log.Timber

sealed class YouMightLikeResult {
    data class Success(val listFeed: ListFeed) : YouMightLikeResult()
    data object Empty : YouMightLikeResult()
}

class YouMightLikeHandler @Inject constructor(
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

    fun getYouMightLikeListFlowable(podcast: Podcast): Flowable<YouMightLikeResult> {
        return isEnabledFlowable()
            .distinctUntilChanged()
            .switchMap { enabled ->
                if (enabled) {
                    getYouMightLikeListSingle(podcast)
                        .toFlowable()
                        .addSubscribedStatusFlowable()
                        .map { listFeed ->
                            if (listFeed.podcasts.isNullOrEmpty()) {
                                YouMightLikeResult.Empty
                            } else {
                                YouMightLikeResult.Success(listFeed)
                            }
                        }
                } else {
                    Flowable.just(YouMightLikeResult.Empty)
                }
            }
            .onErrorReturn { error ->
                Timber.e(error, "Error loading you might like podcasts")
                YouMightLikeResult.Empty
            }
    }

    private fun getYouMightLikeListSingle(podcast: Podcast): Single<ListFeed> = rxSingle {
        listRepository.getYouMightLikePodcasts(podcast.uuid)
            ?: error("Failed to load you might like podcasts")
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
