package au.com.shiftyjelly.pocketcasts.views.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import javax.inject.Inject

class ShareDialogFactory @Inject constructor(
    private val shareActionProvider: ShareActionProvider,
) {
    fun create(
        podcast: Podcast,
        episode: PodcastEpisode?,
        fragmentManager: FragmentManager?,
        context: Context?,
        shouldShowPodcast: Boolean = true,
        forceDarkTheme: Boolean = false,
        analyticsTracker: AnalyticsTracker,
    ) = ShareDialog(podcast, episode, fragmentManager, context, shouldShowPodcast, forceDarkTheme, analyticsTracker, shareActionProvider)
}
