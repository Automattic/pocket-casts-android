package au.com.shiftyjelly.pocketcasts.profile.whatsnew

internal enum class WhatsNewActionEvent(val key: String) {
    OpenPodcasts("open_podcasts"),
    OpenDiscover("open_discover"),
    OpenUpNext("open_up_next"),
    OpenPlaylists("open_playlists"),
    OpenProfile("open_profile"),
    OpenSettings("open_settings"),
    OpenUpsell("open_upsell"),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}
