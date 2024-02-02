package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class NewEpisodeNotificationAction(
    val id: Int,
    @StringRes val labelId: Int,
    @DrawableRes val drawableId: Int,
    @DrawableRes val largeDrawableId: Int,
) {
    Play(
        id = 1,
        labelId = LR.string.play,
        drawableId = IR.drawable.notification_action_play,
        largeDrawableId = IR.drawable.notification_action_play_large,
    ),
    PlayNext(
        id = 2,
        labelId = LR.string.play_next,
        drawableId = IR.drawable.notification_action_playnext,
        largeDrawableId = IR.drawable.notification_action_playnext_large,
    ),
    PlayLast(
        id = 3,
        labelId = LR.string.play_last,
        drawableId = IR.drawable.notification_action_playlast,
        largeDrawableId = IR.drawable.notification_action_playlast_large,
    ),
    Archive(
        id = 4,
        labelId = LR.string.archive,
        drawableId = IR.drawable.notification_action_archive,
        largeDrawableId = IR.drawable.notification_action_archive_large,
    ),
    Download(
        id = 5,
        labelId = LR.string.download,
        drawableId = IR.drawable.notification_action_download,
        largeDrawableId = IR.drawable.notification_action_download_large,
    ),
    ;

    companion object {
        val DefaultValues = listOf(Play, PlayNext, Download)

        fun labels(resources: Resources) = entries.map { resources.getString(it.labelId) }

        fun fromLabels(labels: List<String>, resources: Resources) = labels.mapNotNull { fromLabel(it, resources) }

        private fun fromLabel(label: String, resources: Resources) = entries.find { action ->
            val actionLabel = resources.getString(action.labelId)
            actionLabel == label
        }
    }
}
