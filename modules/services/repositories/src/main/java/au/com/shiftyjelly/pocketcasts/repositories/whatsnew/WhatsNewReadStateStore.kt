package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.SharedPreferences
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import java.time.Instant
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

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in whatsNewKeys) {
            synchronized(this) { _state.value = read() }
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun markAsRead(messageIds: Collection<String>) = update { state ->
        state.copy(
            readMessageIds = state.readMessageIds + messageIds,
            pendingReadMessageIds = state.pendingReadMessageIds + (messageIds - state.readMessageIds),
            pendingUnreadMessageIds = state.pendingUnreadMessageIds - messageIds.toSet(),
        )
    }

    fun markAsUnread(messageIds: Collection<String>) = update { state ->
        state.copy(
            readMessageIds = state.readMessageIds - messageIds.toSet(),
            pendingReadMessageIds = state.pendingReadMessageIds - messageIds.toSet(),
            pendingUnreadMessageIds = state.pendingUnreadMessageIds + (messageIds intersect state.readMessageIds),
        )
    }

    fun markAsUploaded(readMessageIds: Set<String> = emptySet(), unreadMessageIds: Set<String> = emptySet()) = update { state ->
        state.copy(
            pendingReadMessageIds = state.pendingReadMessageIds - readMessageIds,
            pendingUnreadMessageIds = state.pendingUnreadMessageIds - unreadMessageIds,
        )
    }

    fun applyAccountReadState(messageIds: Set<String>, accountReadMessageIds: Set<String>) = update { state ->
        val unreadOnAccount = messageIds - accountReadMessageIds - state.pendingReadMessageIds
        val readOnAccount = accountReadMessageIds - state.pendingUnreadMessageIds
        state.copy(readMessageIds = state.readMessageIds - unreadOnAccount + readOnAccount)
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

    fun startFeed(date: Instant) = update { state ->
        state.copy(feedStartDate = state.feedStartDate ?: date)
    }

    fun forgetReadMessages() = update { state ->
        state.copy(
            readMessageIds = emptySet(),
            pendingReadMessageIds = emptySet(),
            pendingUnreadMessageIds = emptySet(),
        )
    }

    fun reset() = synchronized(this) {
        _state.value = WhatsNewReadState()
        preferences.edit {
            whatsNewKeys.forEach(::remove)
        }
    }

    private fun update(transform: (WhatsNewReadState) -> WhatsNewReadState): Boolean = synchronized(this) {
        val state = transform(_state.value)
        if (state == _state.value) return@synchronized false

        _state.value = state
        preferences.edit {
            putStringSet(READ_KEY, state.readMessageIds)
            putStringSet(SEEN_KEY, state.seenMessageIds)
            putStringSet(LISTED_KEY, state.listedMessageIds)
            putStringSet(RESPONDED_KEY, state.respondedPollIds)
            putStringSet(PENDING_READ_KEY, state.pendingReadMessageIds)
            putStringSet(PENDING_UNREAD_KEY, state.pendingUnreadMessageIds)
            val feedStartDate = state.feedStartDate
            if (feedStartDate == null) {
                remove(FEED_START_DATE_KEY)
            } else {
                putLong(FEED_START_DATE_KEY, feedStartDate.toEpochMilli())
            }
        }
        true
    }

    private fun read() = WhatsNewReadState(
        readMessageIds = preferences.readIds(READ_KEY),
        seenMessageIds = preferences.readIds(SEEN_KEY),
        listedMessageIds = preferences.readIds(LISTED_KEY),
        respondedPollIds = preferences.readIds(RESPONDED_KEY),
        feedStartDate = preferences.takeIf { it.contains(FEED_START_DATE_KEY) }
            ?.getLong(FEED_START_DATE_KEY, 0)
            ?.let(Instant::ofEpochMilli),
        pendingReadMessageIds = preferences.readIds(PENDING_READ_KEY),
        pendingUnreadMessageIds = preferences.readIds(PENDING_UNREAD_KEY),
    )

    private fun SharedPreferences.readIds(key: String) = getStringSet(key, null).orEmpty().toSet()

    private companion object {
        const val READ_KEY = "whatsNewReadMessageIds"
        const val SEEN_KEY = "whatsNewSeenMessageIds"
        const val LISTED_KEY = "whatsNewListedMessageIds"
        const val RESPONDED_KEY = "whatsNewRespondedPollIds"
        const val FEED_START_DATE_KEY = "whatsNewFeedStartDate"
        const val PENDING_READ_KEY = "whatsNewPendingReadMessageIds"
        const val PENDING_UNREAD_KEY = "whatsNewPendingUnreadMessageIds"

        val whatsNewKeys = setOf(
            READ_KEY,
            SEEN_KEY,
            LISTED_KEY,
            RESPONDED_KEY,
            FEED_START_DATE_KEY,
            PENDING_READ_KEY,
            PENDING_UNREAD_KEY,
        )
    }
}
