package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class NewEpisodeNotificationAction(
    val id: Int,
    @StringRes val labelId: Int,
    @DrawableRes val drawableId: Int,
    @DrawableRes val largeDrawableId: Int,
    val notificationAction: String
) {
    PLAY(1, LR.string.play, IR.drawable.notification_action_play, IR.drawable.notification_action_play_large, NotificationBroadcastReceiver.INTENT_ACTION_PLAY_EPISODE),
    PLAY_NEXT(2, LR.string.play_next, IR.drawable.notification_action_playnext, IR.drawable.notification_action_playnext_large, NotificationBroadcastReceiver.INTENT_ACTION_PLAY_NEXT),
    PLAY_LAST(3, LR.string.play_last, IR.drawable.notification_action_playlast, IR.drawable.notification_action_playlast_large, NotificationBroadcastReceiver.INTENT_ACTION_PLAY_LAST),
    ARCHIVE(4, LR.string.archive, IR.drawable.notification_action_archive, IR.drawable.notification_action_archive_large, NotificationBroadcastReceiver.INTENT_ACTION_ARCHIVE),
    DOWNLOAD(5, LR.string.download, IR.drawable.notification_action_download, IR.drawable.notification_action_download_large, NotificationBroadcastReceiver.INTENT_ACTION_DOWNLOAD_EPISODE);

    fun index(): Int {
        return values().indexOf(this)
    }

    companion object {
        val DEFAULT_ACTIONS = listOf(PLAY, PLAY_NEXT, DOWNLOAD)

        fun fromLabel(label: String, resources: Resources): NewEpisodeNotificationAction? {
            values().forEach { action ->
                val actionLabel = resources.getString(action.labelId)
                if (actionLabel == label) {
                    return action
                }
            }
            return null
        }

        fun fromLabels(labels: List<String>, resources: Resources): List<NewEpisodeNotificationAction> {
            val notifications = mutableListOf<NewEpisodeNotificationAction>()
            labels.forEach { label ->
                fromLabel(label, resources)?.let { action ->
                    notifications.add(action)
                }
            }
            return notifications
        }

        fun fromId(id: Int): NewEpisodeNotificationAction {
            NewEpisodeNotificationAction.values().forEach { action ->
                if (action.id == id) {
                    return action
                }
            }
            return PLAY
        }

        fun actionsToString(actions: List<NewEpisodeNotificationAction>): String {
            return actions.joinToString(separator = ",") { it.id.toString() }
        }

        fun actionsFromString(value: String): List<NewEpisodeNotificationAction> {
            return value.splitIgnoreEmpty(",").map { fromId(it.toInt()) }
        }

        fun labels(resources: Resources): List<String> {
            return values().map { resources.getString(it.labelId) }
        }

        fun loadFromSettings(settings: Settings): List<NewEpisodeNotificationAction> {
            return actionsFromString(settings.getNewEpisodeNotificationActions() ?: return DEFAULT_ACTIONS)
        }

        fun saveToSettings(actions: MutableList<NewEpisodeNotificationAction>, settings: Settings) {
            settings.setNewEpisodeNotificationActions(actionsToString(actions))
        }
    }
}
