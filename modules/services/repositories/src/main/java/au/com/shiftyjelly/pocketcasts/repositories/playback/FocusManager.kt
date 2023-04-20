package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

/**
 * Manages the focus of the player by tracking audio focus and audio noisy events.
 */
open class FocusManager(private val settings: Settings, context: Context?) : AudioManager.OnAudioFocusChangeListener {

    companion object {
        // we don't have audio focus, and can't duck
        private const val AUDIO_NO_FOCUS_NO_DUCK = 0
        // we don't have audio focus, and can't duck but focus is going to be given back
        private const val AUDIO_NO_FOCUS_NO_DUCK_TRANSIENT = 1
        // we don't have focus, but can duck (play at a low volume) and focus is going to be given back
        private const val AUDIO_NO_FOCUS_CAN_DUCK_TRANSIENT = 2
        // we have full audio focus
        private const val AUDIO_FOCUSED = 3
    }

    private val audioManager: AudioManager? = if (context == null) null else context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    // track if another app has stolen audio focus
    private var audioFocus: Int = 0
    // track when the time lost as we don't want to resume if it has been too long
    private var timeFocusLost: Long = 0
    private var deviceRemovedWhileFocusLost = false

    // fire events when the focus changes
    var focusChangeListener: FocusChangeListener? = null

    val isFocused: Boolean
        get() = audioFocus == AUDIO_FOCUSED

    val isFocusLost: Boolean
        get() = audioFocus == AUDIO_NO_FOCUS_NO_DUCK || audioFocus == AUDIO_NO_FOCUS_NO_DUCK_TRANSIENT || audioFocus == AUDIO_NO_FOCUS_CAN_DUCK_TRANSIENT

    val isLostTransient: Boolean
        get() = audioFocus == AUDIO_NO_FOCUS_NO_DUCK_TRANSIENT || audioFocus == AUDIO_NO_FOCUS_CAN_DUCK_TRANSIENT

    init {
        registerAudioDeviceListener()
    }

    /**
     * Try to get the system audio focus.
     */
    fun tryToGetAudioFocus(): Boolean {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Trying to gain audio focus")
        //  we already have focus
        if (audioFocus == AUDIO_FOCUSED) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "We already had audio focus")
            return true
        }
        // request focus
        if (audioManager == null) {
            audioFocus = AUDIO_FOCUSED
            return true
        }

        val audioFocusRequest = getAudioFocusRequest()
        val result = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocus = AUDIO_FOCUSED
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Audio focus gained")
            return true
        } else {
            focusChangeListener?.onFocusRequestFailed()
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Couldn't get audio focus")
            return false
        }
    }

    /**
     * Give up the audio focus.
     */
    fun giveUpAudioFocus() {
        if (audioManager == null) {
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Giving up audio focus, null audio manager")
        } else {
            val audioFocusRequest = getAudioFocusRequest()
            val result = AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)

            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Giving up audio focus")

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Giving up audio focus. Request granted")
            } else {
                LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Giving up audio focus request failed")
            }
        }
    }

    private fun getAudioFocusRequest(): AudioFocusRequestCompat {
        val attributes = AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA).build()
        return AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(this)
            .setAudioAttributes(attributes).build()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // map to our own focus status
        /*if (focusChange == AudioManager.AUDIOFOCUS_GAIN ||
            focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ||
            focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ||
            focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        ) {
            // focus gained
            // if not transient only let it resume within 2 minutes
            val shouldResume = (isLostTransient || System.currentTimeMillis() < timeFocusLost + 120000) && !deviceRemovedWhileFocusLost
            audioFocus = AUDIO_FOCUSED
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus gained, should resume $shouldResume. Device removed: $deviceRemovedWhileFocusLost")
            focusChangeListener?.onFocusGain(shouldResume)
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            // focus lost
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK
            } else if (isFocused) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    audioFocus = AUDIO_NO_FOCUS_NO_DUCK_TRANSIENT
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    audioFocus = AUDIO_NO_FOCUS_CAN_DUCK_TRANSIENT
                }
            } // if already paused with a focus lost don't then allow the sound to play ducked
            timeFocusLost = System.currentTimeMillis()
            deviceRemovedWhileFocusLost = false
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus lost. AUDIOFOCUS_LOSS: %s AUDIOFOCUS_LOSS_TRANSIENT: %s AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: %s", focusChange == AudioManager.AUDIOFOCUS_LOSS, focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
            focusChangeListener?.onFocusLoss(canDuck(), isLostTransient)
        } else if (focusChange == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            focusChangeListener?.onFocusRequestFailed()
        } else {
            Timber.w("onAudioFocusChange: Ignoring unsupported focusChange: %d", focusChange)
        }*/
    }

    fun canDuck(): Boolean {
        return audioFocus == AUDIO_NO_FOCUS_CAN_DUCK_TRANSIENT && hasUserAllowedDucking()
    }

    protected open fun hasUserAllowedDucking(): Boolean {
        return settings.canDuckAudioWithNotifications()
    }

    interface FocusChangeListener {
        fun onFocusGain(shouldResume: Boolean)
        fun onFocusLoss(mayDuck: Boolean, transientLoss: Boolean)
        fun onFocusRequestFailed()
    }

    private fun registerAudioDeviceListener() {
        if (audioManager == null) {
            return
        }

        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    for (device in addedDevices) {
                        if (isStandardDeviceType(device)) {
                            continue
                        }
                        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Audio device added: %s, %s", device.productName, deviceTypeToString(device.type))
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                    for (device in removedDevices) {
                        if (isStandardDeviceType(device)) {
                            continue
                        }
                        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Audio device removed: %s, %s", device.productName, deviceTypeToString(device.type))

                        deviceRemovedWhileFocusLost = true
                    }
                }
            },
            null
        )
    }

    private fun isStandardDeviceType(device: AudioDeviceInfo): Boolean {
        return device.type == 1 || device.type == 2 || device.type == 18 || device.type == 15
    }

    private fun deviceTypeToString(type: Int): String {
        val typeString: String
        when (type) {
            0 -> typeString = "Unknown"
            1 -> typeString = "Built-in earpiece"
            2 -> typeString = "Built-in speaker"
            3 -> typeString = "Wired headset"
            4 -> typeString = "Wired headphones"
            5 -> typeString = "Line analog (headphone cable)"
            6 -> typeString = "Line digital"
            7 -> typeString = "Bluetooth sco (telephony)"
            8 -> typeString = "Bluetooth a2dp"
            9 -> typeString = "Hdmi"
            10 -> typeString = "Hdmi arc"
            11 -> typeString = "Usb device"
            12 -> typeString = "Usb accessory"
            13 -> typeString = "Dock"
            14 -> typeString = "Fm"
            15 -> typeString = "Built-in mic"
            16 -> typeString = "Fm tuner"
            17 -> typeString = "Tv tuner"
            18 -> typeString = "Telephony"
            19 -> typeString = "Aux line"
            20 -> typeString = "Over IP"
            21 -> typeString = "Communication with external audio systems"
            22 -> typeString = "USB headset"
            23 -> typeString = "Hearing aid"
            24 -> typeString = "Built-in speaker (safe)"
            25 -> typeString = "Rerouting audio between mixes and system apps"
            26 -> typeString = "BLE headset"
            27 -> typeString = "BLE speaker"
            28 -> typeString = "Echo canceller loopback reference"
            29 -> typeString = "HDMI EARC"
            else -> typeString = "Type not found $type"
        }

        return typeString
    }
}
