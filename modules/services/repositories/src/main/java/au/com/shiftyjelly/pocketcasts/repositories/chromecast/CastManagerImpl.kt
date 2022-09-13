package au.com.shiftyjelly.pocketcasts.repositories.chromecast

import android.content.Context
import androidx.core.content.ContextCompat
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class CastManagerImpl @Inject constructor(@ApplicationContext private val context: Context) : CastManager {

    private val sessionManagerListener = CastSessionManagerListener()
    private var sessionListener: CastManager.SessionListener? = null

    override val isConnectedObservable = BehaviorRelay.create<Boolean>().apply { accept(false) }

    init {
        val executor = ContextCompat.getMainExecutor(context)
        CastContext.getSharedInstance(context, executor)
            .addOnFailureListener { e -> LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Failed to init CastContext shared instance ${e.message}") }
            .addOnSuccessListener { castContext -> castContext.sessionManager.addSessionManagerListener(sessionManagerListener) }
    }

    override suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.Main) {
            castAvailable()
        }
    }

    private fun castAvailable(): Boolean {
        return try {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        } catch (e: Throwable) {
            Timber.e(e)
            false
        }
    }

    override suspend fun isConnected(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                getSessionManager()?.currentCastSession?.isConnected ?: false
            } catch (e: Throwable) {
                Timber.e(e)
                false
            }
        }
    }

    override suspend fun endSession() {
        withContext(Dispatchers.Main) {
            try {
                getSessionManager()?.endCurrentSession(true)
            } catch (e: Throwable) {
                Timber.e(e)
            }
        }
    }

    override suspend fun startSessionListener(sessionListener: CastManager.SessionListener) {
        withContext(Dispatchers.Main) {
            try {
                this@CastManagerImpl.sessionListener = sessionListener
            } catch (e: Throwable) {
                Timber.e(e)
            }
        }
    }

    override suspend fun stopSessionListener() {
        withContext(Dispatchers.Main) {
            sessionListener = null
        }
    }

    override suspend fun isPlaying(): Boolean = withContext(Dispatchers.Main) {
        return@withContext getSessionManager()?.currentCastSession?.remoteMediaClient?.isPlaying == true
    }

    private fun getSessionManager(): SessionManager? {
        if (!castAvailable()) return null
        return try {
            CastContext.getSharedInstance()?.sessionManager
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Couldn't load cast despite it reporting it is available")
            null
        }
    }

    private inner class CastSessionManagerListener : SessionManagerListener<Session> {

        override fun onSessionStarting(session: Session) {
            Timber.i("Cast Session onSessionStarting")
        }

        override fun onSessionStarted(session: Session, s: String) {
            Timber.i("Cast Session onSessionStarted")
            sessionListener?.sessionStarted()
            isConnectedObservable.accept(true)
        }

        override fun onSessionStartFailed(session: Session, i: Int) {
            Timber.i("Cast Session onSessionStartFailed")
        }

        override fun onSessionEnding(session: Session) {
            Timber.i("Cast Session onSessionEnding")
        }

        override fun onSessionEnded(session: Session, i: Int) {
            Timber.i("Cast Session onSessionEnded")
            sessionListener?.sessionEnded()
            isConnectedObservable.accept(false)
        }

        override fun onSessionResuming(session: Session, s: String) {
            Timber.i("Cast Session onSessionResuming")
        }

        override fun onSessionResumed(session: Session, b: Boolean) {
            Timber.i("Cast Session onSessionResumed $sessionListener")
            sessionListener?.sessionReconnected()
            isConnectedObservable.accept(true)
        }

        override fun onSessionResumeFailed(session: Session, i: Int) {
            Timber.i("Cast Session onSessionResumeFailed")
        }

        override fun onSessionSuspended(session: Session, i: Int) {
            Timber.i("Cast Session onSessionSuspended")
            isConnectedObservable.accept(false)
        }
    }
}
