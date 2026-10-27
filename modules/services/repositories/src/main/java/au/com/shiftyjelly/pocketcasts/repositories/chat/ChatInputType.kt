package au.com.shiftyjelly.pocketcasts.repositories.chat

enum class ChatInputType(val value: String) {
    Typed("typed"),
    SummaryPrompt("summary_prompt"),
    SuggestedPrompt("suggested_prompt"),
    ;

    companion object {
        fun fromValue(value: String?) = entries.firstOrNull { it.value == value }
    }
}
