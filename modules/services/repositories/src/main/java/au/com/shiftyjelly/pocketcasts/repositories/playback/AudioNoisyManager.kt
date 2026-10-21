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

    private val mainHandler = Handler(Looper.getMainLooper())

    private val hdmiDisconnected = Runnable { listener?.onHdmiAudioDisconnected() }

    private val hdmiDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.any(::isHdmiSink)) {
                mainHandler.removeCallbacks(hdmiDisconnected)
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any(::isHdmiSink)) {
                mainHandler.removeCallbacks(hdmiDisconnected)
                mainHandler.postDelayed(hdmiDisconnected, HDMI_DISCONNECT_DELAY_MS)
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
            context.getSystemService<AudioManager>()?.registerAudioDeviceCallback(hdmiDeviceCallback, mainHandler)
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
                mainHandler.removeCallbacks(hdmiDisconnected)
            }
        }
    }

    private fun isHdmiSink(device: AudioDeviceInfo) = device.isSink && device.type in HDMI_DEVICE_TYPES

    interface AudioBecomingNoisyListener {
        fun onAudioBecomingNoisy()

        fun onHdmiAudioDisconnected()
    }

    private companion object {
        const val HDMI_DISCONNECT_DELAY_MS = 1_000L

        val HDMI_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC,
        )
    }
}
