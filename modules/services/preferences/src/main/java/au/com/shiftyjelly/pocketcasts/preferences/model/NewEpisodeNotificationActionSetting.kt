package au.com.shiftyjelly.pocketcasts.preferences.model

sealed class NewEpisodeNotificationActionSetting {
    object Default : NewEpisodeNotificationActionSetting() {
        const val stringValue = ""
    }
    class ValueOf(val value: String) : NewEpisodeNotificationActionSetting()
}
