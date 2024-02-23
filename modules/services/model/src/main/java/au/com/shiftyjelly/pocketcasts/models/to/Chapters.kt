package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

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

    fun getNextSelectedChapter(timeMs: Int): Chapter? {
        val currentTimeFinal = if (timeMs < 0) 0 else timeMs
        val items = if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) selectedItems else items
        for (chapter in items) {
            if (chapter.startTime > currentTimeFinal) {
                return chapter
            }
        }
        return null
    }

    fun getPreviousSelectedChapter(timeMs: Int): Chapter? {
        if (items.isEmpty()) {
            return null
        }
        var foundChapter: Chapter? = null
        var lastChapter: Chapter? = null
        val items = if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) selectedItems else items
        for (chapter in items) {
            if (chapter.containsTime(timeMs)) {
                if (foundChapter != null) {
                    lastChapter = foundChapter
                }
                foundChapter = chapter
            } else if (chapter.startTime <= timeMs) {
                lastChapter = chapter
            } else {
                return lastChapter
            }
        }
        return lastChapter
    }

    fun getChapter(time: Int): Chapter? {
        if (isEmpty) {
            return null
        }
        val finalTime = if (time < 0) 0 else time
        var foundChapter: Chapter? = null
        for (chapter in items) {
            if (chapter.containsTime(finalTime)) {
                foundChapter = chapter
            }
        }
        return foundChapter
    }

    fun getChapterIndex(time: Int): Int {
        if (isEmpty) {
            return -1
        }
        val finalTime = if (time < 0) 0 else time
        var foundIndex = -1
        var index = 0
        for (chapter in items) {
            if (chapter.containsTime(finalTime)) {
                foundIndex = index
            }
            index++
        }
        return foundIndex
    }

    fun getList(): List<Chapter> {
        return items
    }

    fun getChapterSummary(time: Int): String {
        val chapterSize = items.size
        val chapterIndex = getChapterIndex(time)
        return if (chapterIndex == -1) "" else "${chapterIndex + 1} of $chapterSize"
    }

    fun isFirstChapter(time: Int): Boolean {
        return getChapterIndex(time) == 0
    }

    fun isLastChapter(time: Int): Boolean {
        return getChapterIndex(time) == items.size - 1
    }
}
