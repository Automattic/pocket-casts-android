package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.time.Duration

data class Chapters(
    private val items: List<Chapter> = emptyList(),
) : List<Chapter> by items {
    private val selectedItems: List<Chapter>
        get() = filter { it.selected }

    fun getNextSelectedChapter(time: Duration): Chapter? {
        val currentTimeFinal = time.coerceAtLeast(Duration.ZERO)
        val items = selectedItems
        for (chapter in items) {
            if (chapter.startTime > currentTimeFinal) {
                return chapter
            }
        }
        return null
    }

    fun getPreviousSelectedChapter(time: Duration): Chapter? {
        if (isEmpty()) {
            return null
        }
        var foundChapter: Chapter? = null
        var lastChapter: Chapter? = null
        val items = selectedItems
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
        return firstOrNull { chapter -> finalTime in chapter }
    }

    fun getChapterIndex(time: Duration): Int {
        val finalTime = time.coerceAtLeast(Duration.ZERO)
        return indexOfFirst { chapter -> finalTime in chapter }
    }

    fun getChapterSummary(time: Duration): ChapterSummaryData {
        val chapterIndex = getChapterIndex(time)
        return ChapterSummaryData(chapterIndex + 1, size)
    }

    fun isFirstChapter(time: Duration): Boolean {
        return getChapterIndex(time) == 0
    }

    fun isLastChapter(time: Duration): Boolean {
        return getChapterIndex(time) == size - 1
    }

    fun skippedChaptersDuration(time: Duration): Duration {
        return items
            .filter { !it.selected && it.endTime > time }
            .fold(Duration.ZERO) { duration, chapter ->
                duration + if (time in chapter) {
                    chapter.endTime - time
                } else {
                    chapter.duration
                }
            }
    }
}

data class ChapterSummaryData(
    val currentIndex: Int = -1,
    val size: Int = 0,
)
