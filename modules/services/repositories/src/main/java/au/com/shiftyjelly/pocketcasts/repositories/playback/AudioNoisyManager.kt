package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

open class AudioNoisyManager(private val context: Context) {

    private var listener: AudioBecomingNoisyListener? = null
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    @Volatile private var receiverRegistered: Boolean = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                listener?.onAudioBecomingNoisy()
            }
        }
    }

    open fun register(listener: AudioBecomingNoisyListener) {
        this.listener = listener
        if (receiverRegistered) {
            return
        }
        receiverRegistered = true
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    open fun unregister() {
        if (receiverRegistered) {
            receiverRegistered = false
            try {
                context.unregisterReceiver(broadcastReceiver)
            } catch (e: IllegalArgumentException) {
                // ignore
            }
        }
    }

    interface AudioBecomingNoisyListener {
        fun onAudioBecomingNoisy()
    }
}
