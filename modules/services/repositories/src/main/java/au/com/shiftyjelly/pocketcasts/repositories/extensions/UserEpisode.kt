package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings

fun UserEpisode.getUrlForArtwork(themeIsDark: Boolean = false, thumbnail: Boolean = false): String {
    if (tintColorIndex == 0 && artworkUrl != null) {
        artworkUrl?.let { return@getUrlForArtwork it }
    }

    val themeType = if (themeIsDark) "dark" else "light"
    val size = if (thumbnail) 280 else 960
    return "${Settings.SERVER_STATIC_URL}/discover/images/artwork/$themeType/$size/$tintColorIndex.png"
}
