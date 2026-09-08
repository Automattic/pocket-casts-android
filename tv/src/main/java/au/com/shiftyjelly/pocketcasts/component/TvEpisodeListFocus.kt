package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Tracks D-pad focus for a list of episode rows so that focus survives a row being removed, e.g.
 * "Remove from Up Next" or archiving while archived episodes are hidden. Without recovery the
 * disposed row leaves the screen with no focus owner and focus falls back to the first focusable.
 *
 * Focus is keyed by episode uuid rather than index so it stays pinned to a stable row while the
 * list reshuffles. The shared requester is only ever bound to the row that is about to be focused,
 * and the focus request retries across frames until that row has attached the requester.
 */
@Composable
internal fun rememberTvEpisodeListFocus(
    episodes: List<PodcastEpisode>,
    listState: LazyListState,
    requestInitialFocus: Boolean,
): TvEpisodeListFocus {
    val focus = remember { TvEpisodeListFocus() }

    if (requestInitialFocus) {
        LaunchedEffect(episodes.isNotEmpty()) {
            if (episodes.isNotEmpty() && !focus.hasRequestedInitialFocus) {
                val index = Snapshot.withoutReadObservation { listState.firstVisibleItemIndex }
                    .coerceIn(0, episodes.lastIndex)
                focus.focusRow(listState, targetUuid = episodes[index].uuid, removedUuid = null)
                focus.hasRequestedInitialFocus = true
            }
        }
    }

    LaunchedEffect(episodes) {
        val removedUuid = focus.pendingRemovalUuid ?: return@LaunchedEffect
        if (episodes.any { it.uuid == removedUuid }) return@LaunchedEffect
        val target = focus.recoveryUuid
        focus.clearPending()
        if (target != null && episodes.any { it.uuid == target }) {
            focus.focusRow(listState, targetUuid = target, removedUuid = removedUuid)
        }
    }
    return focus
}

@Stable
internal class TvEpisodeListFocus {
    private val requester = FocusRequester()

    var hasRequestedInitialFocus by mutableStateOf(false)
        internal set

    private var boundUuid by mutableStateOf<String?>(null)

    var pendingRemovalUuid by mutableStateOf<String?>(null)
        private set
    var recoveryUuid: String? = null
        private set

    fun requesterFor(uuid: String): FocusRequester? = requester.takeIf { uuid == boundUuid }

    fun watchForRemoval(episodes: List<PodcastEpisode>, index: Int) {
        val episode = episodes.getOrNull(index) ?: return
        pendingRemovalUuid = episode.uuid
        recoveryUuid = episodes.getOrNull(index + 1)?.uuid ?: episodes.getOrNull(index - 1)?.uuid
    }

    internal fun clearPending() {
        pendingRemovalUuid = null
    }

    internal suspend fun focusRow(listState: LazyListState, targetUuid: String, removedUuid: String?) {
        boundUuid = targetUuid
        snapshotFlow {
            val keys = listState.layoutInfo.visibleItemsInfo.map { it.key }
            keys.contains(targetUuid) && (removedUuid == null || !keys.contains(removedUuid))
        }.first { it }
        // Re-assert focus across a few frames: the requester may take a frame to attach, and a
        // disposed row makes the platform fall its focus back to the first focusable, which we
        // need to override. The requester stays bound to the target row so it is never left on a
        // stale one. Requesting focus on the already-focused row is a no-op.
        var focused = false
        repeat(FOCUS_ATTEMPTS) {
            focused = runCatching { requester.requestFocus() }.isSuccess || focused
            withFrameNanos {}
        }
        if (!focused) {
            Timber.e("Failed to focus TV episode row $targetUuid after $FOCUS_ATTEMPTS attempts")
        }
    }

    private companion object {
        const val FOCUS_ATTEMPTS = 6
    }
}
