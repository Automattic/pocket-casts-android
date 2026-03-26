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
    playbackManager: PlaybackManager,
    networkConnectionWatcher: NetworkConnectionWatcher,
    @ApplicationContext private val context: Context,
) {
    val playbackIssue: Flow<PlaybackIssueInfo?> = combine(
        playbackManager.playbackStateFlow,
        networkConnectionWatcher.networkCapabilities,
        FeatureFlag.isEnabledFlow(Feature.PLAYBACK_ERROR_INFO_BAR),
    ) { playbackState, networkCapabilities, isEnabled ->
        if (!isEnabled) return@combine null
        resolveIssue(playbackState, networkCapabilities)
    }

    private fun resolveIssue(
        playbackState: PlaybackState,
        networkCapabilities: NetworkCapabilities?,
    ): PlaybackIssueInfo? {
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
