package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class PlaybackIssueType {
    CONNECTION,
    PLAYBACK,
}

data class PlaybackIssueInfo(
    val message: String,
    val type: PlaybackIssueType,
)

@Singleton
class PlaybackIssueManager @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    @ApplicationContext private val context: Context,
) {
    val playbackIssue: Flow<PlaybackIssueInfo?> = combine(
        playbackManager.playbackStateFlow
            .map { it.state to it.lastErrorMessage }
            .distinctUntilChanged(),
        networkConnectionWatcher.networkCapabilities,
        FeatureFlag.isEnabledFlow(Feature.PLAYBACK_ERROR_INFO_BAR),
    ) { (state, lastErrorMessage), networkCapabilities, isEnabled ->
        if (!isEnabled) return@combine null
        resolveIssue(state, lastErrorMessage, networkCapabilities)
    }

    private fun resolveIssue(
        state: PlaybackState.State,
        lastErrorMessage: String?,
        networkCapabilities: NetworkCapabilities?,
    ): PlaybackIssueInfo? {
        val isError = state == PlaybackState.State.ERROR
        val isOffline = networkCapabilities == null ||
            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        return when {
            playbackState.isError && (isOffline || playbackState.isConnectionError) -> PlaybackIssueInfo(
                message = context.getString(LR.string.error_playback_offline),
                type = PlaybackIssueType.CONNECTION,
            )

            playbackState.isError -> PlaybackIssueInfo(
                message = context.getString(LR.string.error_episode_not_available),
                type = PlaybackIssueType.PLAYBACK,
            )

            else -> null
        }
    }
}
