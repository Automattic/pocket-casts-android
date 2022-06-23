package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor

object PlaylistHelper {
    fun updateImageView(playlist: Playlist, imageView: ImageView?) {
        if (imageView == null) {
            return
        }
        imageView.setImageResource(playlist.drawableId)
        val color = playlist.getColor(imageView.context)
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
    }
}
