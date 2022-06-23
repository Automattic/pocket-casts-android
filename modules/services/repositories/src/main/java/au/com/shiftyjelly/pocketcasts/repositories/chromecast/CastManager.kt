package au.com.shiftyjelly.pocketcasts.repositories.chromecast

import io.reactivex.Observable

interface CastManager {

    val isConnectedObservable: Observable<Boolean>

    suspend fun isAvailable(): Boolean
    suspend fun isConnected(): Boolean
    suspend fun endSession()
    suspend fun startSessionListener(sessionListener: SessionListener)
    suspend fun stopSessionListener()
    suspend fun isPlaying(): Boolean

    interface SessionListener {
        fun sessionStarted()
        fun sessionEnded()
        fun sessionReconnected()
    }
}
