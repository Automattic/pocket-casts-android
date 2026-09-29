package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.userEpisodePlaceholderArtworkUrl

fun UserEpisode.getUrlForArtwork(themeIsDark: Boolean = false, thumbnail: Boolean = false): String {
    if (tintColorIndex == 0 && artworkUrl != null) {
        artworkUrl?.let { return@getUrlForArtwork it }
    }

    val size = if (thumbnail) 280 else 960
    return userEpisodePlaceholderArtworkUrl(tintColorIndex, isDarkTheme = themeIsDark, size = size)
}
