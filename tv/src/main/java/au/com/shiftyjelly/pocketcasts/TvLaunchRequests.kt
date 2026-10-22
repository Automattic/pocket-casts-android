package au.com.shiftyjelly.pocketcasts

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Singleton
class TvLaunchRequests @Inject constructor() {
    private val openNowPlayingChannel = Channel<Unit>(Channel.CONFLATED)

    val openNowPlaying: Flow<Unit> = openNowPlayingChannel.receiveAsFlow()

    fun requestOpenNowPlaying() {
        openNowPlayingChannel.trySend(Unit)
    }
}
