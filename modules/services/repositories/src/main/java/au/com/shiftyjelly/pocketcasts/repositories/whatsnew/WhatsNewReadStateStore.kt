package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.SharedPreferences
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@Singleton
class WhatsNewReadStateStore @Inject constructor(
    @PublicSharedPreferences private val preferences: SharedPreferences,
) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<WhatsNewReadState> = _state.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in whatsNewKeys) {
            synchronized(this) { _state.value = read() }
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

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

    fun markAsResponded(pollId: String, optionId: String) = update { state ->
        state.copy(
            respondedPollIds = state.respondedPollIds + pollId,
            pollAnswers = state.pollAnswers + (pollId to optionId),
        )
    }

    fun reset() = synchronized(this) {
        _state.value = WhatsNewReadState()
        preferences.edit {
            whatsNewKeys.forEach(::remove)
        }
    }

    private fun update(transform: (WhatsNewReadState) -> WhatsNewReadState) = synchronized(this) {
        val state = transform(_state.value)
        if (state == _state.value) return@synchronized

        _state.value = state
        preferences.edit {
            putStringSet(READ_KEY, state.readMessageIds)
            putStringSet(SEEN_KEY, state.seenMessageIds)
            putStringSet(LISTED_KEY, state.listedMessageIds)
            putStringSet(RESPONDED_KEY, state.respondedPollIds)
            putString(POLL_ANSWERS_KEY, JSONObject(state.pollAnswers).toString())
        }
    }

    private fun read() = WhatsNewReadState(
        readMessageIds = preferences.readIds(READ_KEY),
        seenMessageIds = preferences.readIds(SEEN_KEY),
        listedMessageIds = preferences.readIds(LISTED_KEY),
        respondedPollIds = preferences.readIds(RESPONDED_KEY),
        pollAnswers = preferences.readPollAnswers(),
    )

    private fun SharedPreferences.readIds(key: String) = getStringSet(key, null).orEmpty().toSet()

    private fun SharedPreferences.readPollAnswers(): Map<String, String> {
        val json = getString(POLL_ANSWERS_KEY, null) ?: return emptyMap()
        return try {
            val answers = JSONObject(json)
            answers.keys().asSequence().associateWith(answers::getString)
        } catch (e: JSONException) {
            Timber.w(e, "Could not read the What's New poll answers")
            emptyMap()
        }
    }

    private companion object {
        const val READ_KEY = "whatsNewReadMessageIds"
        const val SEEN_KEY = "whatsNewSeenMessageIds"
        const val LISTED_KEY = "whatsNewListedMessageIds"
        const val RESPONDED_KEY = "whatsNewRespondedPollIds"
        const val POLL_ANSWERS_KEY = "whatsNewPollAnswers"

        val whatsNewKeys = setOf(READ_KEY, SEEN_KEY, LISTED_KEY, RESPONDED_KEY, POLL_ANSWERS_KEY)
    }
}
