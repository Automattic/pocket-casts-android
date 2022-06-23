package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.android.exoplayer2.metadata.id3.ChapterFrame
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.metadata.id3.UrlLinkFrame
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

class EpisodeFileMetadata(val filenamePrefix: String? = null) {

    companion object {
        private const val TAG_APIC = "APIC"
        private const val TAG_TITLE = "TIT2"
        private const val TAG_LENGTH = "TLEN"

        private val START_TIME_COMPARATOR = Comparator<Chapter> { chapterOne, chapterTwo ->
            chapterOne.startTime.compareTo(chapterTwo.startTime)
        }
    }

    var chapters = Chapters()
    var embeddedArtworkPath: String? = null
    var embeddedTitle: String? = null
    var embeddedLength: Long? = null

    fun read(trackSelections: TrackSelectionArray?, settings: Settings, context: Context) {
        return read(trackSelections, settings.getUseEmbeddedArtwork(), context)
    }

    fun read(trackSelections: TrackSelectionArray?, loadArtwork: Boolean, context: Context) {
        val newChapters = mutableListOf<Chapter>()
        embeddedArtworkPath = null

        if (trackSelections == null) {
            return
        }
        try {
            for (i in 0 until trackSelections.length) {
                val selection = trackSelections.get(i) ?: continue
                for (j in 0 until selection.length()) {
                    val metadata = selection.getFormat(j).metadata ?: continue
                    for (k in 0 until metadata.length()) {
                        val frame = metadata.get(k)
                        if (frame is ChapterFrame) {
                            val chapter = convertFrameToChapter(frame, newChapters.size, context) ?: continue
                            newChapters.add(chapter)
                        } else if (frame is ApicFrame && TAG_APIC == frame.id && loadArtwork) {
                            val file = File.createTempFile("$filenamePrefix-podcast_embedded_artwork", "jpg", context.cacheDir)
                            val filePath = saveToDisk(frame.pictureData, file, context)
                            this.embeddedArtworkPath = filePath
                        } else if (frame is TextInformationFrame && TAG_TITLE == frame.id) {
                            this.embeddedTitle = frame.value
                        } else if (frame is TextInformationFrame && TAG_LENGTH == frame.id) {
                            this.embeddedLength = frame.value.toLongOrNull()
                        }
                    }
                }
            }
            // sort the chapters by start time
            Collections.sort(newChapters, START_TIME_COMPARATOR)
            newChapters.forEachIndexed { index, chapter ->
                chapter.index = index + 1
            }
            chapters = Chapters(newChapters)
        } catch (e: Exception) {
            Timber.e(e, "Unable to read chapters from ID3 tags.")
        }
    }

    private fun convertFrameToChapter(frame: ChapterFrame?, chapterIndex: Int, context: Context): Chapter? {
        if (frame == null || frame.startTimeMs < 0) {
            return null
        }
        var title = ""
        var url: String? = null
        var imagePath: String? = null
        var mimeType: String? = null
        for (i in 0 until frame.subFrameCount) {
            val subFrame = frame.getSubFrame(i)
            if (subFrame is TextInformationFrame) {
                if ("TIT2" == subFrame.id) {
                    title = subFrame.value
                }
            } else if (subFrame is UrlLinkFrame) {
                url = subFrame.url
            } else if (subFrame is ApicFrame) {
                val fileName = String.format("$filenamePrefix-chapterImage%s", chapterIndex)
                val file = File(context.cacheDir, fileName)
                val filePath = saveToDisk(subFrame.pictureData, file, context)
                imagePath = filePath
                mimeType = subFrame.mimeType
            }
        }
        return Chapter(
            title,
            url?.toHttpUrlOrNull(),
            frame.startTimeMs,
            frame.endTimeMs,
            frame.startOffset,
            frame.endOffset,
            imagePath,
            mimeType
        )
    }

    private fun saveToDisk(pictureData: ByteArray, file: File, context: Context): String {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(pictureData, 0, pictureData.size, options)

        val windowHeightPx = context.resources.displayMetrics.heightPixels
        val windowWidthPx = context.resources.displayMetrics.widthPixels
        val windowSizePx = Math.min(windowWidthPx, windowHeightPx)

        val height = options.outHeight
        val width = options.outWidth
        // resize the chapter artwork if it is too large for example "Área de Transferência - Episode 013"
        if (width > windowSizePx || height > windowSizePx) {
            val scaleSize = Math.round(Math.log(windowSizePx / Math.max(height, width).toDouble()) / Math.log(0.5)).toInt()
            val scale = Math.pow(2.0, scaleSize.toDouble()).toInt()

            options = BitmapFactory.Options()
            options.inSampleSize = scale
            options.inPreferredConfig = Bitmap.Config.RGB_565

            var fileOutputStream: FileOutputStream? = null
            try {
                val scaledBitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.size, options)
                fileOutputStream = FileOutputStream(file)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream)
                scaledBitmap.recycle()
            } catch (e: Throwable) {
                Timber.e(e, "Failed to scale chapter image.")
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                }
            }
        } else {
            var outputStream: BufferedOutputStream? = null
            try {
                outputStream = BufferedOutputStream(FileOutputStream(file))
                outputStream.write(pictureData)
            } catch (e: IOException) {
                Timber.e(e)
            } finally {
                try {
                    outputStream?.flush()
                } catch (e: Exception) {
                }

                try {
                    outputStream?.close()
                } catch (e: Exception) {
                }
            }
        }

        return file.absolutePath
    }
}
