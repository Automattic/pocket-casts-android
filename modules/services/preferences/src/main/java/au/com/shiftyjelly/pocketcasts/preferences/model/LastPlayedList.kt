package au.com.shiftyjelly.pocketcasts.preferences.model

sealed class LastPlayedList(open val uuid: String?) {
    class Uuid(override val uuid: String) : LastPlayedList(uuid)
    object None : LastPlayedList(null)

    companion object {
        val default = None

        fun fromString(uuid: String?): LastPlayedList =
            if (uuid == null) {
                None
            } else {
                Uuid(uuid)
            }
    }
}
