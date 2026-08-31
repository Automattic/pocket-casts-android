package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/**
 * Debug-only per-VAD-segment capture behind the [Feature.WAKE_WORD_DEBUG_CAPTURE]
 * flag. For every non-silent VAD segment (score > 0) it saves, to the public
 * Downloads dir, the raw WAV and a log-Mel spectrogram PNG, both named
 * `<score>-<MMdd_HHmmss>.*` so a segment's audio and spectrogram pair by base
 * name. A ring buffer keeps only the newest [MAX_SEGMENTS] segments. See
 * recognition-pipeline.md "Debug instrumentation".
 */
object WakeWordSegmentCapture {

    private const val MEL_BINS = 32
    private const val MAX_SEGMENTS = 100
    private const val MAX_PNG_WIDTH = 1024
    private const val SUBDIR = "WakeWordDebug"
    private val dateFormat = SimpleDateFormat("MMdd_HHmmss", Locale.US)

    fun capture(context: Context, samples: FloatArray, sampleRateHz: Int, score: Float) {
        if (!FeatureFlag.isEnabled(Feature.WAKE_WORD_DEBUG_CAPTURE)) return
        // Silence/quiet segments score ~0 and carry no signal to diagnose. Exclude
        // anything that formats to "0.000" (score < 0.0005), not just exact 0, so
        // near-silence doesn't flood the capture dir.
        if (score < 0.0005f) return

        // Short name: <score>-MMdd_HHmmss, e.g. "0.673-0806_183545".
        val base = "${"%.3f".format(score)}-${dateFormat.format(Date())}"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$SUBDIR/"
                writeViaMediaStore(context.contentResolver, "$base.wav", relativePath, "audio/wav") { out ->
                    writeWav(out, samples, sampleRateHz)
                }
                val mel = WakeWordJni.nativeGetLogMel(samples, sampleRateHz)
                if (mel != null) {
                    writeViaMediaStore(context.contentResolver, "$base.png", relativePath, "image/png") { out ->
                        writeMelPng(out, mel)
                    }
                } else {
                    Timber.w("Wake word debug: no mel frames for segment")
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val sub = File(dir, SUBDIR)
                if (!sub.exists() && !sub.mkdirs()) {
                    Timber.w("Wake word debug: cannot create $sub")
                    return
                }
                FileOutputStream(File(sub, "$base.wav")).use { writeWav(it, samples, sampleRateHz) }
                val mel = WakeWordJni.nativeGetLogMel(samples, sampleRateHz)
                if (mel != null) {
                    FileOutputStream(File(sub, "$base.png")).use { writeMelPng(it, mel) }
                }
            }
            enforceRingBuffer(context.contentResolver)
        } catch (e: Exception) {
            Timber.w(e, "Wake word debug: segment capture failed")
        }
    }

    /** Insert one file into the Downloads MediaStore collection. */
    private fun writeViaMediaStore(
        resolver: ContentResolver,
        displayName: String,
        relativePath: String,
        mimeType: String,
        writer: (OutputStream) -> Unit,
    ) {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return
        try {
            resolver.openOutputStream(uri)?.use { writer(it) }
        } finally {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    /** Write a 16-bit mono PCM WAV from normalized float samples in [-1, 1]. */
    private fun writeWav(out: OutputStream, samples: FloatArray, sampleRateHz: Int) {
        val dataSize = samples.size * 2
        val fileSize = 36 + dataSize
        out.write("RIFF".toByteArray())
        out.write(int32Le(fileSize))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(int32Le(16))
        out.write(int16Le(1)) // PCM
        out.write(int16Le(1)) // mono
        out.write(int32Le(sampleRateHz))
        out.write(int32Le(sampleRateHz * 2)) // byte rate
        out.write(int16Le(2)) // block align
        out.write(int16Le(16)) // bits per sample
        out.write("data".toByteArray())
        out.write(int32Le(dataSize))
        for (s in samples) {
            val v = (s * 32767f).toInt().coerceIn(-32768, 32767)
            out.write(int16Le(v))
        }
    }

    /** Render a log-Mel matrix (flat time_frames * 32) to a PNG stream. */
    private fun writeMelPng(out: OutputStream, mel: FloatArray) {
        val frames = mel.size / MEL_BINS
        if (frames <= 0) return
        val width = minOf(frames, MAX_PNG_WIDTH)
        val stride = frames / width // subsample factor when width capped
        val bitmap = Bitmap.createBitmap(width, MEL_BINS, Bitmap.Config.ARGB_8888)

        // Normalize to the 1st..99th percentile for contrast robustness.
        val sorted = mel.copyOf().sorted()
        val lo = sorted[(sorted.size * 0.01).toInt()]
        val hi = sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.lastIndex)]
        val span = (hi - lo).coerceAtLeast(1e-6f)

        for (x in 0 until width) {
            val frame = x * stride
            for (b in 0 until MEL_BINS) {
                val v = mel[frame * MEL_BINS + b]
                val t = ((v - lo) / span).coerceIn(0f, 1f)
                bitmap.setPixel(x, b, heatColor(t))
            }
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        bitmap.recycle()
    }

    /** Keep only the newest [MAX_SEGMENTS] segments (two files each) in Downloads. */
    private fun enforceRingBuffer(resolver: ContentResolver) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$SUBDIR/"
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val args = arrayOf(relativePath)
        // Oldest first: names start with the score (not time), so order by the
        // file's modification time, not the display name.
        val files = mutableListOf<Pair<Long, String>>() // id to base name
        resolver.query(collection, projection, selection, args, "${MediaStore.MediaColumns.DATE_MODIFIED} ASC")
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1)
                    files.add(id to name.substringBeforeLast('.'))
                }
            }
        val bases = files.map { it.second }.distinct().toMutableList()
        while (bases.size > MAX_SEGMENTS) {
            val oldestBase = bases.removeAt(0)
            val doomed = files.filter { it.second == oldestBase }
            files.removeAll { it.second == oldestBase }
            for ((id, _) in doomed) {
                resolver.delete(collection, "${MediaStore.MediaColumns._ID}=?", arrayOf(id.toString()))
            }
        }
    }

    private fun heatColor(t: Float): Int {
        // black -> dark red -> orange -> yellow -> white (classic heat map)
        return when {
            t < 0.25f -> lerpColor(Color.rgb(0, 0, 0), Color.rgb(90, 0, 0), t / 0.25f)
            t < 0.5f -> lerpColor(Color.rgb(90, 0, 0), Color.rgb(230, 120, 0), (t - 0.25f) / 0.25f)
            t < 0.75f -> lerpColor(Color.rgb(230, 120, 0), Color.rgb(255, 230, 90), (t - 0.5f) / 0.25f)
            else -> lerpColor(Color.rgb(255, 230, 90), Color.WHITE, (t - 0.75f) / 0.25f)
        }
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val r = ((a shr 16 and 0xFF) + t * ((b shr 16 and 0xFF) - (a shr 16 and 0xFF))).toInt()
        val g = ((a shr 8 and 0xFF) + t * ((b shr 8 and 0xFF) - (a shr 8 and 0xFF))).toInt()
        val bl = ((a and 0xFF) + t * ((b and 0xFF) - (a and 0xFF))).toInt()
        return Color.rgb(r, g, bl)
    }

    private fun int16Le(v: Int): ByteArray = byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())

    private fun int32Le(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )
}
