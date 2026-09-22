package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.SharedPreferences
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WhatsNewReadStateStore @Inject constructor(
    @PublicSharedPreferences private val preferences: SharedPreferences,
) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<WhatsNewReadState> = _state.asStateFlow()

    fun markAsRead(messageIds: Collection<String>) = update { state ->
        state.copy(readMessageIds = state.readMessageIds + messageIds)
    }

    fun markAsSeen(messageIds: Collection<String>) = update { state ->
        state.copy(seenMessageIds = state.seenMessageIds + messageIds)
    }

    fun markAsListed(messageIds: Collection<String>) = update { state ->
        state.copy(
            listedMessageIds = state.listedMessageIds + messageIds,
            seenMessageIds = state.seenMessageIds + messageIds,
        )
    }

    fun markAsResponded(pollId: String) = update { state ->
        state.copy(respondedPollIds = state.respondedPollIds + pollId)
    }

    fun reset() {
        _state.value = WhatsNewReadState()
        preferences.edit {
            keys.forEach(::remove)
        }
    }

    private fun update(transform: (WhatsNewReadState) -> WhatsNewReadState) {
        val state = transform(_state.value)
        if (state == _state.value) return

        _state.value = state
        preferences.edit {
            putStringSet(READ_KEY, state.readMessageIds)
            putStringSet(SEEN_KEY, state.seenMessageIds)
            putStringSet(LISTED_KEY, state.listedMessageIds)
            putStringSet(RESPONDED_KEY, state.respondedPollIds)
        }
    }

    private fun read() = WhatsNewReadState(
        readMessageIds = preferences.readIds(READ_KEY),
        seenMessageIds = preferences.readIds(SEEN_KEY),
        listedMessageIds = preferences.readIds(LISTED_KEY),
        respondedPollIds = preferences.readIds(RESPONDED_KEY),
    )

    private fun SharedPreferences.readIds(key: String) = getStringSet(key, null).orEmpty().toSet()

    private companion object {
        const val READ_KEY = "whatsNewReadMessageIds"
        const val SEEN_KEY = "whatsNewSeenMessageIds"
        const val LISTED_KEY = "whatsNewListedMessageIds"
        const val RESPONDED_KEY = "whatsNewRespondedPollIds"

        val keys = listOf(READ_KEY, SEEN_KEY, LISTED_KEY, RESPONDED_KEY)
    }
}
