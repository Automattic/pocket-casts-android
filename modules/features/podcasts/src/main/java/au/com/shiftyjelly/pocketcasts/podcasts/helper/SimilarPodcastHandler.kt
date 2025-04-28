package au.com.shiftyjelly.pocketcasts.podcasts.helper

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import kotlinx.coroutines.rx2.rxSingle

class SimilarPodcastHandler @Inject constructor(
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

    fun getSimilarPodcastsFlowable(podcast: Podcast): Flowable<List<DiscoverPodcast>> {
        enabledObservable.onNext(false)
        return isEnabledFlowable()
            .distinctUntilChanged()
            .switchMap { enabled ->
                if (enabled) {
                    getSimilarPodcastsSingle(podcast)
                        .toFlowable()
                        .addSubscribedStatusFlowable()
                } else {
                    Flowable.just(emptyList())
                }
            }
    }

    private fun getSimilarPodcastsSingle(podcast: Podcast) = rxSingle {
        listRepository.getSimilarPodcasts(podcast.uuid)?.podcasts ?: emptyList()
    }

    private fun Flowable<List<DiscoverPodcast>>.addSubscribedStatusFlowable(): Flowable<List<DiscoverPodcast>> {
        return switchMap { podcasts ->
            podcastManager.getSubscribedPodcastUuidsRxSingle()
                .toFlowable()
                .mergeWith(podcastManager.podcastSubscriptionsRxFlowable())
                .map { subscribedList ->
                    podcasts.map { podcast ->
                        podcast.copy(isSubscribed = subscribedList.contains(podcast.uuid))
                    }
                }
        }
    }
}
