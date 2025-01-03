package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlin.time.Duration

data class Chapters(
    private val items: List<Chapter> = emptyList(),
) {

    val isEmpty: Boolean
        get() = items.isEmpty()

    val size: Int
        get() = items.size

    private val selectedItems: List<Chapter>
        get() = items.filter { it.selected }

    val lastChapter: Chapter?
        get() = items.getOrNull(items.size - 1)

    fun getNextSelectedChapter(time: Duration): Chapter? {
        val currentTimeFinal = time.coerceAtLeast(Duration.ZERO)
        val items = if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) selectedItems else items
        for (chapter in items) {
            if (chapter.startTime > currentTimeFinal) {
                return chapter
            }
        }
        return null
    }

    fun getPreviousSelectedChapter(time: Duration): Chapter? {
        if (items.isEmpty()) {
            return null
        }
        var foundChapter: Chapter? = null
        var lastChapter: Chapter? = null
        val items = if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) selectedItems else items
        for (chapter in items) {
            if (time in chapter) {
                if (foundChapter != null) {
                    lastChapter = foundChapter
                }
                foundChapter = chapter
            } else if (chapter.startTime <= time) {
                lastChapter = chapter
            } else {
                return lastChapter
            }
        }
        return lastChapter
    }

    fun getChapter(time: Duration): Chapter? {
        val finalTime = time.coerceAtLeast(Duration.ZERO)
        return items.firstOrNull { chapter -> finalTime in chapter }
    }

    fun getChapterIndex(time: Duration): Int {
        val finalTime = time.coerceAtLeast(Duration.ZERO)
        return items.indexOfFirst { chapter -> finalTime in chapter }
    }

    fun getList(): List<Chapter> {
        return items
    }

    fun getChapterSummary(time: Duration): ChapterSummaryData {
        val chapterSize = items.size
        val chapterIndex = getChapterIndex(time)
        return ChapterSummaryData(chapterIndex + 1, chapterSize)
    }

    fun isFirstChapter(time: Duration): Boolean {
        return getChapterIndex(time) == 0
    }

    fun isLastChapter(time: Duration): Boolean {
        return getChapterIndex(time) == items.size - 1
    }

    fun skippedChaptersDuration(time: Duration): Duration {
        return if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) {
            items
                .filter { !it.selected && it.endTime > time }
                .fold(Duration.ZERO) { duration, chapter ->
                    duration + if (time in chapter) {
                        chapter.endTime - time
                    } else {
                        chapter.duration
                    }
                }
        } else {
            Duration.ZERO
        }
    }

    fun toDbChapters(
        episodeId: String,
        isEmbedded: Boolean,
    ) = items.map { chapter ->
        DbChapter(
            episodeUuid = episodeId,
            startTimeMs = chapter.startTime.inWholeMilliseconds,
            endTimeMs = chapter.endTime.inWholeMilliseconds,
            title = chapter.title,
            imageUrl = chapter.imagePath,
            url = chapter.url?.toString(),
            isEmbedded = isEmbedded,
        )
    }
}

data class ChapterSummaryData(
    val currentIndex: Int = -1,
    val size: Int = 0,
)
