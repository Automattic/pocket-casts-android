package au.com.shiftyjelly.pocketcasts

import android.content.Context
import android.content.Intent
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.media.MediaIntentReceiver

class PocketCastsMediaIntentReceiver : MediaIntentReceiver() {
    private lateinit var application: PocketCastsApplication

    override fun onReceive(context: Context, intent: Intent) {
        application = context.applicationContext as PocketCastsApplication
        super.onReceive(context, intent)
    }

    override fun onReceiveActionRewind(session: Session, skipStepMs: Long) {
        seek(session, -application.settings.skipBackInSecs.value * 1000L)
    }

    override fun onReceiveActionForward(session: Session, skipStepMs: Long) {
        seek(session, application.settings.skipForwardInSecs.value * 1000L)
    }

    private fun seek(session: Session, deltaMs: Long) {
        if (deltaMs == 0L) return
        val castSession = session as? CastSession ?: return
        if (!castSession.isConnected) return
        val client = castSession.remoteMediaClient ?: return
        if (client.isLiveStream || client.isPlayingAd) return
        val seekOptions = MediaSeekOptions.Builder()
            .setPosition(client.approximateStreamPosition + deltaMs)
            .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
            .build()
        client.seek(seekOptions)
    }
}
