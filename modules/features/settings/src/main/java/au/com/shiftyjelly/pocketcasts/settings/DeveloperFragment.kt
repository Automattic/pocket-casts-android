package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.UpNextSyncJob
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class DeveloperFragment : PreferenceFragmentCompat(), CoroutineScope {

    @Inject lateinit var theme: Theme
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var userEpisodeManager: UserEpisodeManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var syncManager: SyncManager

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_developer, rootKey)

        preferenceManager.findPreference<Preference>("causeNotification")?.setOnPreferenceClickListener {
            triggerNotification()
            true
        }

        preferenceManager.findPreference<Preference>("forceRefresh")?.setOnPreferenceClickListener {
            forceRefresh()
            true
        }

        preferenceManager.findPreference<Preference>("deleteOld")?.setOnPreferenceClickListener {
            deleteOldEpisodes()
            true
        }

        preferenceManager.findPreference<Preference>("syncUpNext")?.setOnPreferenceClickListener {
            syncUpNext()
            true
        }

        preferenceManager.findPreference<Preference>("launchSubFlow")?.setOnPreferenceClickListener {
            launchSubFlow()
            true
        }
    }

    private fun launchSubFlow() {
        subscriptionManager.observeProductDetails()
            .firstOrError()
            .subscribeBy(
                onSuccess = {
                    if (it is ProductDetailsState.Loaded) {
                        it.productDetails.firstOrNull()?.let { productDetails ->
                            val isFreeTrialEligible = subscriptionManager.isFreeTrialEligible()
                            Subscription.fromProductDetails(productDetails, isFreeTrialEligible)?.let { subscription ->
                                subscriptionManager.launchBillingFlow(requireActivity(), productDetails, subscription.offerToken)
                            } ?: Timber.d("Subscription is null")
                        } ?: Timber.d("Products list is empty")
                    } else {
                        Timber.d("Couldn't get sku details")
                    }
                },
                onError = {
                    Timber.e("Couldn't load products")
                }
            )
    }

    private fun syncUpNext() {
        UpNextSyncJob.run(syncManager, requireActivity())
    }

    private fun deleteOldEpisodes() {
        launch(Dispatchers.Default) {
            try {
                val podcasts = podcastManager.findSubscribed()
                for (podcast in podcasts) {
                    // find first podcast with more than one episode
                    val episodes = episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)
                    if (episodes.size <= 1) {
                        continue
                    } else {
                        // remove the latest episode
                        val episodeToDelete = episodes.first()
                        val newLatest = episodes.getOrNull(1)
                        Timber.i("Deleted episode ${podcast.title} - ${episodeToDelete.title}")
                        episodeManager.deleteEpisodeWithoutSync(episodeToDelete, playbackManager)

                        podcast.latestEpisodeUuid = newLatest?.uuid
                        podcast.latestEpisodeDate = newLatest?.publishedDate
                        podcastManager.updatePodcast(podcast)

                        continue
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun forceRefresh() {
        podcastManager.refreshPodcasts("dev")
    }

    private fun triggerNotification() {
        launch(Dispatchers.Default) {
            try {
                val podcasts = podcastManager.findSubscribed()
                val countNotificationsOn = podcasts.count { it.isShowNotifications }
                if (countNotificationsOn == 0) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(activity, "No notifications turned on", Toast.LENGTH_LONG).show()
                    }
                } else {
                    for (podcast in podcasts) {
                        if (podcast.isShowNotifications) {
                            // find first podcast with more than one episode
                            val episodes = episodeManager.findEpisodesByPodcastOrderedByPublishDate(podcast)
                            if (episodes.size <= 1) {
                                continue
                            } else {
                                val episode = episodes[1]
                                // link the second oldest to the podcast
                                episodeManager.markAsNotPlayed(episode)
                                podcastManager.updateLatestEpisode(podcast, episode)
                                // remove the latest episode
                                val episodeToDelete = episodes[0]
                                Timber.i("Creating a notification for ${podcast.title} - ${episodeToDelete.title}")
                                episodeManager.deleteEpisodeWithoutSync(episodeToDelete, playbackManager)
                                settings.setNotificationLastSeenToNow()
                                continue
                            }
                        }
                    }
                    forceRefresh()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findToolbar().setup(title = getString(LR.string.settings_developer), navigationIcon = BackArrow, activity = activity, theme = theme)
    }
}
