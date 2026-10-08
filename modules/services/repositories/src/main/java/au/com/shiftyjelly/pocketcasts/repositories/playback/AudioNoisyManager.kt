package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService

open class AudioNoisyManager(
    private val context: Context,
    private val pausesOnHdmiDisconnect: Boolean = false,
) {

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

    private val hdmiDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any { it.isSink && it.type in HDMI_DEVICE_TYPES }) {
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
        if (pausesOnHdmiDisconnect) {
            context.getSystemService<AudioManager>()?.registerAudioDeviceCallback(hdmiDeviceCallback, Handler(Looper.getMainLooper()))
        }
    }

    open fun unregister() {
        if (receiverRegistered) {
            receiverRegistered = false
            try {
                context.unregisterReceiver(broadcastReceiver)
            } catch (e: IllegalArgumentException) {
                // ignore
            }
            if (pausesOnHdmiDisconnect) {
                context.getSystemService<AudioManager>()?.unregisterAudioDeviceCallback(hdmiDeviceCallback)
            }
        }
    }

    interface AudioBecomingNoisyListener {
        fun onAudioBecomingNoisy()
    }

    private companion object {
        val HDMI_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC,
        )
    }
}
