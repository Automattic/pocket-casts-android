package au.com.shiftyjelly.pocketcasts.repositories.chat

enum class ChatAnswerRating(val value: String) {
    Positive("positive"),
    Negative("negative"),
    ;

    companion object {
        fun fromValue(value: String?) = entries.find { it.value == value }
    }
}
