package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor

object PlaylistHelper {
    fun updateImageView(smartPlaylist: SmartPlaylist, imageView: ImageView?) {
        if (imageView == null) {
            return
        }
        imageView.setImageResource(smartPlaylist.drawableId)
        val color = smartPlaylist.getColor(imageView.context)
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
    }
}
