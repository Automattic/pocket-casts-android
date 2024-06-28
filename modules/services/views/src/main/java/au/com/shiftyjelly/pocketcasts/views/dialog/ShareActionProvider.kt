package au.com.shiftyjelly.pocketcasts.views.dialog

import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

interface ShareActionProvider {
    fun clipAction(
        podcastEpisode: PodcastEpisode,
        podcast: Podcast,
        fragmentManager: FragmentManager,
        source: SourceView,
    )
}
