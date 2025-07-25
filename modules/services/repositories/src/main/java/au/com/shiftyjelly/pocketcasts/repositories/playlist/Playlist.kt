package au.com.shiftyjelly.pocketcasts.repositories.playlist

sealed interface Playlist {
    companion object {
        const val NEW_RELEASES_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
        const val IN_PROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"
    }
}
