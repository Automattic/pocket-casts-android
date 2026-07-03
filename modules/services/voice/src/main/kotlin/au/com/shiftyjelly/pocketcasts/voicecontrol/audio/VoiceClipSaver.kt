package au.com.shiftyjelly.pocketcasts.voicecontrol.audio

import android.os.Environment
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceUtteranceClip
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/**
 * Saves a [VoiceUtteranceClip] as a WAV file in the public Downloads directory.
 *
 * File name format: VoiceCommand_YYYYMMDD_HHmmss_transcript.wav
 */
object VoiceClipSaver {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun save(clip: VoiceUtteranceClip, transcript: String) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) {
            Timber.w("Cannot create Downloads directory")
            return
        }

        val timestamp = dateFormat.format(Date())
        val sanitized = transcript.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
        val file = File(dir, "VoiceCommand_${timestamp}_$sanitized.wav")

        try {
            FileOutputStream(file).use { out ->
                val allSamples = clip.frames.flatMap { frame -> frame.samples.toList() }
                val sampleRate = clip.sampleRateHz
                val channels = 1
                val bitsPerSample = 16
                val dataSize = allSamples.size * 2 // 2 bytes per short
                val fileSize = 36 + dataSize

                // WAV header
                out.write("RIFF".toByteArray())
                out.write(int32Le(fileSize))
                out.write("WAVE".toByteArray())
                out.write("fmt ".toByteArray())
                out.write(int32Le(16)) // chunk size
                out.write(int16Le(1)) // PCM format
                out.write(int16Le(channels)) // mono
                out.write(int32Le(sampleRate)) // sample rate
                out.write(int32Le(sampleRate * channels * bitsPerSample / 8)) // byte rate
                out.write(int16Le(channels * bitsPerSample / 8)) // block align
                out.write(int16Le(bitsPerSample)) // bits per sample
                out.write("data".toByteArray())
                out.write(int32Le(dataSize))

                // PCM sample data
                for (sample in allSamples) {
                    out.write(int16Le(sample.toInt()))
                }
            }
            Timber.i("Voice clip saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save voice clip")
        }
    }

    private fun int16Le(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
    )

    private fun int32Le(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )
}
