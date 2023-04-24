package au.com.shiftyjelly.pocketcasts.views.helper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BatteryRestrictionsSettingsFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class WarningsHelper @Inject constructor(
    @ActivityContext private val activity: Context,
    private val downloadManager: DownloadManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    private val systemBatteryRestrictions: SystemBatteryRestrictions,
    private val userEpisodeManager: UserEpisodeManager,
    private val episodeAnalytics: EpisodeAnalytics,
) {

    @OptIn(DelicateCoroutinesApi::class)
    fun streamingWarningDialog(
        episode: BaseEpisode,
        snackbarParentView: View? = null,
        playbackSource: AnalyticsSource
    ): ConfirmationDialog {
        return streamingWarningDialog(onConfirm = {
            GlobalScope.launch {
                playbackManager.playNow(episode = episode, forceStream = true, playbackSource = playbackSource)
                showBatteryWarningSnackbarIfAppropriate(snackbarParentView)
            }
        })
    }

    fun streamingWarningDialog(onConfirm: () -> Unit): ConfirmationDialog {
        val titleRes =
            if (Network.isWifiConnection(activity)) LR.string.stream_warning_title_metered_wifi else LR.string.stream_warning_title_not_wifi
        return ConfirmationDialog()
            .setTitle(activity.getString(titleRes))
            .setSummary(activity.getString(LR.string.stream_warning_summary))
            .setIconId(IR.drawable.ic_wifi)
            .setButtonType(ConfirmationDialog.ButtonType.Normal(activity.getString(LR.string.stream_warning_button)))
            .setOnConfirm(onConfirm)
    }

    fun downloadWarning(episodeUuid: String, from: String): ConfirmationDialog {
        val titleRes =
            if (Network.isWifiConnection(activity)) LR.string.download_warning_title_metered_wifi else LR.string.download_warning_title_on_wifi
        return ConfirmationDialog()
            .setIconId(IR.drawable.ic_wifi)
            .setTitle(activity.getString(titleRes))
            .setSummary(activity.getString(LR.string.download_warning_on_wifi_summary))
            .setButtonType(ConfirmationDialog.ButtonType.Normal(activity.getString(LR.string.download_warning_on_wifi_button)))
            .setOnConfirm { download(episodeUuid, waitForWifi = false, from = from) }
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(activity.getString(LR.string.download_warning_on_wifi_later)))
            .setOnSecondary { download(episodeUuid, waitForWifi = true, from = from) }
    }

    fun uploadWarning(episodeUuid: String, source: AnalyticsSource): ConfirmationDialog {
        val titleRes =
            if (Network.isWifiConnection(activity)) LR.string.profile_cloud_upload_warning_title_metered_wifi else LR.string.profile_cloud_upload_warning_title_on_wifi
        return ConfirmationDialog()
            .setIconId(IR.drawable.ic_wifi)
            .setTitle(activity.getString(titleRes))
            .setSummary(activity.getString(LR.string.profile_cloud_upload_warning_summary))
            .setButtonType(ConfirmationDialog.ButtonType.Normal(activity.getString(LR.string.profile_cloud_upload_warning_button)))
            .setOnConfirm { upload(episodeUuid, waitForWifi = false, source = source) }
            .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(activity.getString(LR.string.profile_cloud_upload_warning_later)))
            .setOnSecondary { upload(episodeUuid, waitForWifi = true, source = source) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun upload(episodeUuid: String, waitForWifi: Boolean, source: AnalyticsSource) {
        GlobalScope.launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let {
                if (it !is UserEpisode) return@let

                if (!it.isUploaded && !it.isQueuedForUpload && !it.isUploading) {
                    userEpisodeManager.uploadToServer(it, waitForWifi)
                    episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UPLOAD_QUEUED, source = source, uuid = it.uuid)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun download(episodeUuid: String, waitForWifi: Boolean, from: String) {
        GlobalScope.launch {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let {
                if (it.isDownloading) {
                    episodeManager.stopDownloadAndCleanUp(episodeUuid, from)
                } else if (!it.isDownloaded) {
                    if (!waitForWifi) {
                        it.autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                    }
                    downloadManager.addEpisodeToQueue(it, from, true)
                    launch {
                        episodeManager.unarchive(it)
                    }
                }
            }
        }
    }

    fun showBatteryWarningSnackbarIfAppropriate(snackbarParentView: View? = null) {
        val timesToShow = settings.getTimesToShowBatteryWarning()
        val shouldShow = timesToShow > 0 && !systemBatteryRestrictions.isUnrestricted()
        if (shouldShow) {
            settings.setTimesToShowBatteryWarning(timesToShow - 1)

            val fragmentHostListener = activity as FragmentHostListener
            showBatteryWarningSnackbar(
                snackbarParentView = snackbarParentView ?: fragmentHostListener.snackBarView(),
                openFragment = { fragmentHostListener.showBottomSheet(it) }
            )
        }
    }

    // Even though the Snackbar javadocs explicitly say that a custom duration is allowed, lint prohibits it
    @SuppressLint("WrongConstant")
    private fun showBatteryWarningSnackbar(
        snackbarParentView: View,
        openFragment: (Fragment) -> Unit
    ) {
        // Setting an extra-long duration since this is such a high-priority notification
        val snackbar = Snackbar.make(
            snackbarParentView,
            LR.string.player_battery_warning_snackbar,
            EXTRA_LONG_SNACKBAR_DURATION_MS
        )

        snackbar.setAction(
            snackbarParentView.resources.getString(LR.string.player_battery_warning_snackbar_action)
                .uppercase(Locale.getDefault())
        ) {
            val fragment = BatteryRestrictionsSettingsFragment.newInstance(closeButton = true)
            openFragment(fragment)
        }

        snackbar.show()
    }

    companion object {
        private const val EXTRA_LONG_SNACKBAR_DURATION_MS: Int = 6000
    }
}
