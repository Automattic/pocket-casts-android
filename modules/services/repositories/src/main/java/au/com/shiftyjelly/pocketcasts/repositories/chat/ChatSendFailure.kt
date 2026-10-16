package au.com.shiftyjelly.pocketcasts.repositories.chat

import com.squareup.moshi.JsonClass

enum class ChatSendFailure(val value: String) {
    Network("network"),
    Server("server"),
    ;

    companion object {
        fun fromValue(value: String?) = entries.firstOrNull { it.value == value }
    }
}

@JsonClass(generateAdapter = true)
internal data class UserMessageMetadata(
    val failure: String? = null,
    val inputType: String? = null,
)
