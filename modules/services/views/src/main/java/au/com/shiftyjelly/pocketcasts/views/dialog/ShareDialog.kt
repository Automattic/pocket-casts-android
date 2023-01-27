package au.com.shiftyjelly.pocketcasts.views.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper.ShareType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class ShareDialog(
    private val podcast: Podcast,
    private val episode: Episode?,
    private val fragmentManager: FragmentManager?,
    private val context: Context?,
    private val shouldShowPodcast: Boolean = true,
    private val forceDarkTheme: Boolean = false,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {

    private val source = AnalyticsSource.EPISODE_DETAILS
    fun show() {
        if (fragmentManager == null || context == null) {
            return
        }
        val dialog = OptionsDialog()
            .setForceDarkTheme(forceDarkTheme)
        if (shouldShowPodcast) {
            dialog.addCheckedOption(
                titleId = LR.string.share_podcast,
                click = {
                    SharePodcastHelper(
                        podcast,
                        null,
                        null,
                        context,
                        ShareType.PODCAST,
                        source,
                        analyticsTracker
                    ).showShareDialogDirect()
                }
            )
        }
        if (episode != null) {
            dialog.addCheckedOption(
                titleId = LR.string.podcast_share_episode,
                click = {
                    SharePodcastHelper(
                        podcast,
                        episode,
                        null,
                        context,
                        ShareType.EPISODE,
                        source,
                        analyticsTracker
                    ).showShareDialogDirect()
                }
            )
            dialog.addCheckedOption(
                titleId = LR.string.podcast_share_current_position,
                click = {
                    SharePodcastHelper(
                        podcast,
                        episode,
                        episode.playedUpTo,
                        context,
                        ShareType.CURRENT_TIME,
                        source,
                        analyticsTracker
                    ).showShareDialogDirect()
                }
            )
            if (episode.isDownloaded) {
                dialog.addCheckedOption(
                    titleId = LR.string.podcast_share_open_file_in,
                    click = {
                        SharePodcastHelper(
                            podcast,
                            episode,
                            episode.playedUpTo,
                            context,
                            ShareType.EPISODE_FILE,
                            source,
                            analyticsTracker
                        ).sendFile()
                    }
                )
            }
        }
        dialog.show(fragmentManager, "share_dialog")
    }
}
