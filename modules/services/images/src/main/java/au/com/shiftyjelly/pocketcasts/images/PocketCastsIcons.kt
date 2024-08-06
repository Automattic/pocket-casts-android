package au.com.shiftyjelly.pocketcasts.images

import androidx.compose.ui.graphics.vector.ImageVector
import au.com.shiftyjelly.pocketcasts.images.icons.StarEmpty
import au.com.shiftyjelly.pocketcasts.images.icons.StarFull
import au.com.shiftyjelly.pocketcasts.images.icons.StarHalf

private var icons: List<ImageVector>? = null

object PocketCastsIcons {
    val AllIcons: List<ImageVector>
        get() {
            var iconsLoaded = icons
            if (iconsLoaded != null) {
                return iconsLoaded
            }
            iconsLoaded = listOf(StarFull, StarEmpty, StarHalf)
            icons = iconsLoaded
            return iconsLoaded
        }
}
