package au.com.shiftyjelly.pocketcasts.views.dialog

import androidx.fragment.app.DialogFragment
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

interface ShareDialogFactory {
    fun shareEpisode(podcast: Podcast, episode: PodcastEpisode, source: SourceView): DialogFragment
}
