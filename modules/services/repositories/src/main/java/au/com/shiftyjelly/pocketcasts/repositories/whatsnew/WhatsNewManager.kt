package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalog
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WhatsNewManager {
    val catalog: StateFlow<WhatsNewCatalog?>

    val readState: StateFlow<WhatsNewReadState>

    val feedMessages: Flow<List<WhatsNewMessage>>

    val hasUnlistedMessages: Flow<Boolean>

    val hasUnseenMessages: Flow<Boolean>

    suspend fun refreshIfNeeded()

    suspend fun refresh()

    fun markAsRead(messageIds: Collection<String>)

    fun markAsSeen(messageIds: Collection<String>)

    fun markAsListed(messageIds: Collection<String>)

    fun markAsResponded(pollId: String)

    fun resetReadState()
}
