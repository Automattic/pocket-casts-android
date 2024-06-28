package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.id3.UrlLinkFrame
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

class EpisodeFileMetadata(val filenamePrefix: String? = null) {

    companion object {
        private const val TAG_APIC = "APIC"
        private const val TAG_TITLE = "TIT2"
        private const val TAG_LENGTH = "TLEN"

        private val START_TIME_COMPARATOR = Comparator<Chapter> { chapterOne, chapterTwo ->
            chapterOne.startTime.compareTo(chapterTwo.startTime)
        }

        internal fun artworkCacheFile(
            context: Context,
            filePrefix: String,
        ) = File(context.cacheDir, "$filePrefix-podcast_embedded_artwork.jpg")
    }

    var chapters = Chapters()
    var embeddedArtworkPath: String? = null
    var embeddedTitle: String? = null
    var embeddedLength: Long? = null

    @UnstableApi
    fun read(tracks: Tracks?, useEpisodeArtwork: Boolean, context: Context) {
        val newChapters = mutableListOf<Chapter>()
        embeddedArtworkPath = null

        if (tracks == null) {
            return
        }
        try {
            for (i in 0 until tracks.groups.size) {
                val group = tracks.groups[i] ?: continue
                for (j in 0 until tracks.groups.size) {
                    val metadata = group.getTrackFormat(j).metadata ?: continue
                    for (k in 0 until metadata.length()) {
                        val frame = metadata.get(k)
                        if (frame is ChapterFrame) {
                            val chapter = convertFrameToChapter(frame, newChapters.size, context) ?: continue
                            newChapters.add(chapter)
                        } else if (frame is ApicFrame && TAG_APIC == frame.id && useEpisodeArtwork) {
                            val file = artworkCacheFile(context, filenamePrefix.toString())
                            val filePath = saveToDisk(frame.pictureData, file, context)
                            if (filePath != null) {
                                this.embeddedArtworkPath = filePath
                            }
                        } else if (frame is TextInformationFrame && TAG_TITLE == frame.id) {
                            this.embeddedTitle = frame.values.firstOrNull()
                        } else if (frame is TextInformationFrame && TAG_LENGTH == frame.id) {
                            this.embeddedLength = frame.values.firstOrNull()?.toLongOrNull()
                        }
                    }
                }
            }
            // sort the chapters by start time
            val indexedChapters = newChapters.sortedWith(START_TIME_COMPARATOR).mapIndexed { index, chapter ->
                chapter.copy(index = index + 1)
            }
            chapters = Chapters(indexedChapters)
        } catch (e: Exception) {
            Timber.e(e, "Unable to read chapters from ID3 tags.")
        }
    }

    @UnstableApi
    private fun convertFrameToChapter(frame: ChapterFrame?, chapterIndex: Int, context: Context): Chapter? {
        if (frame == null || frame.startTimeMs < 0) {
            return null
        }
        var title = ""
        var url: String? = null
        var imagePath: String? = null
        for (i in 0 until frame.subFrameCount) {
            val subFrame = frame.getSubFrame(i)
            if (subFrame is TextInformationFrame) {
                if ("TIT2" == subFrame.id) {
                    title = subFrame.values.firstOrNull() ?: ""
                }
            } else if (subFrame is UrlLinkFrame) {
                url = subFrame.url
            } else if (subFrame is ApicFrame) {
                val fileName = String.format("$filenamePrefix-chapterImage%s", chapterIndex)
                val file = File(context.cacheDir, fileName)
                val filePath = saveToDisk(subFrame.pictureData, file, context)
                if (filePath != null) {
                    imagePath = filePath
                }
            }
        }
        return Chapter(
            title = title,
            url = url?.toHttpUrlOrNull(),
            startTime = frame.startTimeMs.milliseconds,
            endTime = frame.endTimeMs.milliseconds,
            imagePath = imagePath,
        )
    }

    private fun saveToDisk(pictureData: ByteArray, file: File, context: Context): String? {
        val sampleSize = 4
        val options = BitmapFactory.Options().apply {
            // parse the image at a smaller sample size to save memory
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.size, options)
        // check the embedded artwork can be decoded
        if (bitmap == null) {
            Timber.i("Failed to decode embedded artwork.")
            runCatching { file.delete() }
            return null
        }

        try {
            val windowHeightPx = context.resources.displayMetrics.heightPixels
            val windowWidthPx = context.resources.displayMetrics.widthPixels
            val windowSizePx = Math.min(windowWidthPx, windowHeightPx)

            val height = options.outHeight * sampleSize
            val width = options.outWidth * sampleSize
            // resize the chapter artwork if it is too large for example "Área de Transferência - Episode 013"
            if (width > windowSizePx || height > windowSizePx) {
                val scaleSize = Math.round(Math.log(windowSizePx / Math.max(height, width).toDouble()) / Math.log(0.5)).toInt()
                val scale = Math.pow(2.0, scaleSize.toDouble()).toInt()

                val resizeOptions = BitmapFactory.Options().apply {
                    inSampleSize = scale
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                FileOutputStream(file).use { outputStream ->
                    val scaledBitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.size, resizeOptions)
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    scaledBitmap.recycle()
                }
            } else {
                BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                    outputStream.write(pictureData)
                }
            }
            return file.absolutePath
        } catch (e: IOException) {
            Timber.e(e)
            runCatching { file.delete() }
        }
        return null
    }
}
