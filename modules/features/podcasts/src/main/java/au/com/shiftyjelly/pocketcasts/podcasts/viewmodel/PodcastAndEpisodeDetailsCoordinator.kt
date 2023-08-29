package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastAndEpisodeDetailsCoordinator @Inject constructor() {
    var onEpisodeDetailsDismissed: (() -> Unit)? = null
}
