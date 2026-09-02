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
        seek(session, rewindDeltaMs(application.settings.skipBackInSecs.value))
    }

    override fun onReceiveActionForward(session: Session, skipStepMs: Long) {
        seek(session, forwardDeltaMs(application.settings.skipForwardInSecs.value))
    }

    private fun seek(session: Session, deltaMs: Long) {
        val castSession = session as? CastSession ?: return
        if (!castSession.isConnected) return
        val client = castSession.remoteMediaClient ?: return
        val targetPositionMs = seekTargetPositionMs(
            approximatePositionMs = client.approximateStreamPosition,
            deltaMs = deltaMs,
            isLiveStream = client.isLiveStream,
            isPlayingAd = client.isPlayingAd,
        ) ?: return
        client.seek(
            MediaSeekOptions.Builder()
                .setPosition(targetPositionMs)
                .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
                .build(),
        )
    }
}

internal fun rewindDeltaMs(skipBackInSecs: Int): Long = -skipBackInSecs.toLong() * 1000L

internal fun forwardDeltaMs(skipForwardInSecs: Int): Long = skipForwardInSecs.toLong() * 1000L

internal fun seekTargetPositionMs(
    approximatePositionMs: Long,
    deltaMs: Long,
    isLiveStream: Boolean,
    isPlayingAd: Boolean,
): Long? {
    if (deltaMs == 0L || isLiveStream || isPlayingAd) return null
    return approximatePositionMs + deltaMs
}
