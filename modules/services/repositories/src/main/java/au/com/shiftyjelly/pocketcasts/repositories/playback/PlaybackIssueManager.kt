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

data class PlaybackIssueInfo(
    val message: String,
    val onClick: (() -> Unit)? = null,
)

@Singleton
class PlaybackIssueManager @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val networkConnectionWatcher: NetworkConnectionWatcher,
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
            playbackState.isError && isOffline -> PlaybackIssueInfo(
                message = context.getString(LR.string.error_playback_offline),
            )

            playbackState.isError && playbackState.lastErrorMessage != null -> PlaybackIssueInfo(
                message = playbackState.lastErrorMessage,
            )

            playbackState.isError -> PlaybackIssueInfo(
                message = context.getString(LR.string.error_check_your_internet_connection),
            )

            else -> null
        }
    }
}
