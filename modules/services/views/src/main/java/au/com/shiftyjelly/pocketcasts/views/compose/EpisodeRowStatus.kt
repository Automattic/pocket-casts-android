package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeLeft
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal enum class EpisodeStatusIconTint {
    Downloaded,
    Secondary,
}

internal data class EpisodeRowStatus(
    val text: String,
    val description: String = "",
    @DrawableRes val iconId: Int? = null,
    val iconTint: EpisodeStatusIconTint = EpisodeStatusIconTint.Secondary,
    val progressCircle: ProgressCircleState? = null,
    val isBuffering: Boolean = false,
) {
    val contentDescription get() = description.ifEmpty { text }

    companion object {
        fun from(episode: PodcastEpisode, rowData: EpisodeRowData, context: Context): EpisodeRowStatus {
            val status = downloadStatus(episode, rowData.downloadProgress, context)
            val playbackState = rowData.playbackState
            return if (playbackState.episodeUuid == episode.uuid && playbackState.isBuffering) {
                status.copy(
                    text = context.getString(LR.string.episode_row_buffering),
                    description = "",
                    iconId = null,
                    isBuffering = true,
                )
            } else {
                status
            }
        }

        private fun downloadStatus(episode: PodcastEpisode, downloadProgress: Int, context: Context): EpisodeRowStatus {
            fun timeLeft() = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)

            fun iconStatus(@StringRes textId: Int, @DrawableRes iconId: Int) = EpisodeRowStatus(context.getString(textId), iconId = iconId)

            return when {
                episode.isDownloaded -> timeLeft().toStatus(IR.drawable.ic_downloaded, EpisodeStatusIconTint.Downloaded)

                episode.isDownloading -> EpisodeRowStatus(
                    text = context.getString(LR.string.episode_row_downloading, downloadProgress),
                    progressCircle = ProgressCircleState.forPercent(downloadProgress / 100.0f),
                )

                episode.isDownloadFailure -> iconStatus(LR.string.episode_row_download_failed, IR.drawable.ic_download_failed_row)

                episode.isWaitingForPower -> iconStatus(LR.string.episode_row_waiting_for_power, IR.drawable.ic_waitingforpower)

                episode.isWaitingForStorage -> iconStatus(LR.string.episode_row_waiting_for_storage, IR.drawable.ic_waitingforstorage)

                episode.isWaitingForWifi -> iconStatus(LR.string.episode_row_waiting_for_wifi, IR.drawable.ic_waitingforwifi)

                episode.isQueuedForDownload -> iconStatus(LR.string.episode_row_queued, IR.drawable.ic_waitingforwifi)

                episode.isArchived -> {
                    val archived = context.getString(LR.string.archived)
                    val timeLeft = timeLeft()
                    EpisodeRowStatus(
                        text = "$archived · ${timeLeft.text}",
                        description = "$archived. ${timeLeft.description}",
                        iconId = IR.drawable.ic_archive,
                    )
                }

                episode.playErrorDetails != null -> EpisodeRowStatus(episode.playErrorDetails.orEmpty(), iconId = IR.drawable.ic_alert_small)

                else -> timeLeft().toStatus()
            }
        }

        private fun TimeLeft.toStatus(
            @DrawableRes iconId: Int? = null,
            iconTint: EpisodeStatusIconTint = EpisodeStatusIconTint.Secondary,
        ) = EpisodeRowStatus(text, description, iconId, iconTint)
    }
}

internal fun episodeRowContentDescription(
    episode: PodcastEpisode,
    date: CharSequence,
    status: EpisodeRowStatus,
    isInUpNext: Boolean,
    context: Context,
) = buildList {
    add(episode.title)
    add(date.toString())
    add(status.contentDescription)
    if (episode.isInProgress) {
        add(context.getString(LR.string.in_progress))
    } else if (isInUpNext) {
        add(context.getString(LR.string.episode_in_up_next))
    }
    if (episode.isDownloaded) {
        add(context.getString(LR.string.downloaded))
    }
    if (episode.isDownloading) {
        add(context.getString(LR.string.episode_downloading))
    }
    if (episode.isStarred) {
        add(context.getString(LR.string.starred))
    }
}.joinToString(separator = ". ", postfix = ".")
