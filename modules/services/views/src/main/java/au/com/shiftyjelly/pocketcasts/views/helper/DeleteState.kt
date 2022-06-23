package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class DeleteState {
    object DeviceOnly : DeleteState()
    object Everywhere : DeleteState()
    object Cloud : DeleteState()
}

object CloudDeleteHelper {
    fun getDeleteState(episode: UserEpisode): DeleteState {
        return getDeleteState(episode.isDownloaded, episode.isDownloaded && episode.isUploaded)
    }

    fun getDeleteState(isDownloaded: Boolean, isBoth: Boolean): DeleteState {
        return when {
            isBoth -> DeleteState.Everywhere
            isDownloaded -> DeleteState.DeviceOnly
            else -> DeleteState.Cloud
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun deleteEpisode(episode: UserEpisode, deleteState: DeleteState, playbackManager: PlaybackManager, episodeManager: EpisodeManager, userEpisodeManager: UserEpisodeManager) {
        GlobalScope.launch(Dispatchers.Default) {
            when (deleteState) {
                is DeleteState.DeviceOnly -> {
                    if (episode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
                        episodeManager.deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false)
                    } else {
                        userEpisodeManager.delete(episode, playbackManager)
                    }
                }
                is DeleteState.Cloud -> {
                    userEpisodeManager.removeFromCloud(episode)
                    if (!episode.isDownloaded) {
                        userEpisodeManager.delete(episode, playbackManager)
                    }
                }
                is DeleteState.Everywhere -> {
                    userEpisodeManager.removeFromCloud(episode)
                    userEpisodeManager.delete(episode, playbackManager)
                }
            }
        }
    }

    fun getDeleteDialog(episode: UserEpisode, deleteState: DeleteState, deleteFunction: (UserEpisode, DeleteState) -> Unit, resources: Resources): ConfirmationDialog {
        val listDeleteFunction: (List<UserEpisode>, DeleteState) -> Unit = { episodes, state ->
            deleteFunction(episodes.first(), state)
        }
        return getDeleteDialog(listOf(episode), deleteState, listDeleteFunction, resources)
    }

    fun getDeleteDialog(episodes: List<UserEpisode>, deleteState: DeleteState, deleteFunction: (List<UserEpisode>, DeleteState) -> Unit, resources: Resources): ConfirmationDialog {
        val confirmationDialog = ConfirmationDialog()
            .setTitle(resources.getString(LR.string.profile_delete_file_title))
            .setSummary(resources.getStringPlural(count = episodes.size, singular = LR.string.profile_cloud_delete_files_singular, plural = LR.string.profile_cloud_delete_files_plural))
            .setIconId(R.drawable.ic_delete)
            .setIconTint(null)

        when (deleteState) {
            is DeleteState.DeviceOnly -> {
                confirmationDialog
                    .setTitle(resources.getString(LR.string.profile_delete_from_device_title))
                    .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.profile_delete_from_device)))
                    .setOnConfirm { deleteFunction(episodes, deleteState) }
            }
            is DeleteState.Cloud -> {
                confirmationDialog
                    .setTitle(resources.getString(LR.string.profile_delete_from_cloud_title))
                    .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.profile_delete_from_cloud)))
                    .setOnConfirm { deleteFunction(episodes, deleteState) }
            }
            is DeleteState.Everywhere -> {
                confirmationDialog
                    .setTitle(resources.getString(LR.string.profile_delete_file_title))
                    .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.profile_delete_everywhere)))
                    .setOnConfirm { deleteFunction(episodes, DeleteState.Everywhere) }
                    .setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal(resources.getString(LR.string.profile_delete_from_device_only)))
                    .setOnSecondary { deleteFunction(episodes, DeleteState.DeviceOnly) }
            }
        }

        return confirmationDialog
    }
}
