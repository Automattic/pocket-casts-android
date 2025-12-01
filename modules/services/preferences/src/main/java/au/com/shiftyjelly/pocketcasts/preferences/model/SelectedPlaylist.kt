package au.com.shiftyjelly.pocketcasts.preferences.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SelectedPlaylist(
    val uuid: String,
    val type: String,
)
