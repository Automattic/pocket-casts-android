package au.com.shiftyjelly.pocketcasts.views.dialog

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper.ShareType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class ShareDialog(
    private val podcast: Podcast,
    private val episode: PodcastEpisode?,
    private val fragmentManager: FragmentManager?,
    private val context: Context?,
    private val shouldShowPodcast: Boolean,
    private val forceDarkTheme: Boolean,
    private val analyticsTracker: AnalyticsTracker,
    private val shareActionProvider: ShareActionProvider,
) {

    init {
        if (context is Application) {
            // Cannot use application context here because it will cause a crash when
            // the show method tries to start a new activity.
            throw IllegalArgumentException("ShareDialog cannot use the application context")
        }
    }

    // If the share dialog is not appearing, make sure you're setting an appropriate fragmentManager
    // when constructing this class, i.e., you might need a parentFragmentManager instead of a childFragmentManager
    fun show(sourceView: SourceView) {
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
                        null,
                        context,
                        ShareType.PODCAST,
                        sourceView,
                        analyticsTracker,
                    ).showShareDialogDirect()
                },
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
                        null,
                        context,
                        ShareType.EPISODE,
                        sourceView,
                        analyticsTracker,
                    ).showShareDialogDirect()
                },
            )
            dialog.addCheckedOption(
                titleId = LR.string.podcast_share_current_position,
                click = {
                    SharePodcastHelper(
                        podcast,
                        episode,
                        episode.playedUpTo.seconds,
                        null,
                        context,
                        ShareType.CURRENT_TIME,
                        sourceView,
                        analyticsTracker,
                    ).showShareDialogDirect()
                },
            )
            if (FeatureFlag.isEnabled(Feature.SHARE_CLIPS)) {
                dialog.addCheckedOption(
                    titleId = LR.string.podcast_share_clip,
                    click = {
                        shareActionProvider.clipAction(episode, podcast, fragmentManager)
                    },
                )
            }
            if (episode.isDownloaded) {
                dialog.addCheckedOption(
                    titleId = LR.string.podcast_share_open_file_in,
                    click = {
                        SharePodcastHelper(
                            podcast,
                            episode,
                            episode.playedUpTo.seconds,
                            null,
                            context,
                            ShareType.EPISODE_FILE,
                            sourceView,
                            analyticsTracker,
                        ).sendFile()
                    },
                )
            }
        }
        dialog.show(fragmentManager, "share_dialog")
    }
}
