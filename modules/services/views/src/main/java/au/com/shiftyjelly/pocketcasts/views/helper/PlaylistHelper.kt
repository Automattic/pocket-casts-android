package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor

object PlaylistHelper {
    fun updateImageView(playlist: PlaylistEntity, imageView: ImageView?) {
        if (imageView == null) {
            return
        }
        imageView.setImageResource(playlist.icon.drawableId)
        val color = playlist.icon.getColor(imageView.context)
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
    }
}
