package au.com.shiftyjelly.pocketcasts.voicecontrol.audio

data class PcmAudioFrame(
    val samples: ShortArray,
    val sampleRateHz: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PcmAudioFrame) return false

        return sampleRateHz == other.sampleRateHz && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRateHz
        return result
    }

    override fun toString(): String {
        return "PcmAudioFrame(samples=${samples.contentToString()}, sampleRateHz=$sampleRateHz)"
    }
}
