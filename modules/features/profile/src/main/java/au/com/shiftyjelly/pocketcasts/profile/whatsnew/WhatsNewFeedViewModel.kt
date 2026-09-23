package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WhatsNewFeedViewModel @Inject constructor(
    private val manager: WhatsNewManager,
    settings: Settings,
) : ViewModel() {
    private val loadState = MutableStateFlow(LoadState.Loading)
    private val isRefreshing = MutableStateFlow(false)

    internal val uiState: StateFlow<UiState> = combine(
        manager.feedMessages.onEach { messages -> manager.markAsListed(messages.map { it.id }) },
        manager.readState,
        manager.catalog,
        loadState,
        isRefreshing,
    ) { messages, readState, catalog, loadState, isRefreshing ->
        UiState(
            items = messages.map { message ->
                WhatsNewFeedItem(
                    id = message.id,
                    type = message.type,
                    title = message.title,
                    publishedAt = message.publishedAt,
                    isUnread = !readState.isRead(message.id),
                )
            },
            loadState = if (catalog != null) LoadState.Loaded else loadState,
            isRefreshing = isRefreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    val bottomInset: StateFlow<Int> = settings.bottomInset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        load()
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            try {
                manager.refresh()
            } finally {
                isRefreshing.value = false
            }
            failIfNothingLoaded()
        }
    }

    fun retry() {
        loadState.value = LoadState.Loading
        load()
    }

    fun onMessageClick(id: String) {
        manager.markAsRead(listOf(id))
    }

    private fun load() {
        viewModelScope.launch {
            manager.refreshIfNeeded()
            failIfNothingLoaded()
        }
    }

    private fun failIfNothingLoaded() {
        if (manager.catalog.value == null) {
            loadState.value = LoadState.Failed
        }
    }

    internal data class UiState(
        val items: List<WhatsNewFeedItem> = emptyList(),
        val loadState: LoadState = LoadState.Loading,
        val isRefreshing: Boolean = false,
    )

    internal enum class LoadState {
        Loading,
        Loaded,
        Failed,
    }
}

internal data class WhatsNewFeedItem(
    val id: String,
    val type: WhatsNewMessageType,
    val title: String,
    val publishedAt: Instant,
    val isUnread: Boolean,
)
