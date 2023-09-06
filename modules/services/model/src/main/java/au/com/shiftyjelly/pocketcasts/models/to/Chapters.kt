package au.com.shiftyjelly.pocketcasts.models.to

data class Chapters(private val items: List<Chapter> = emptyList()) {

    val isEmpty: Boolean
        get() = items.isEmpty()

    val size: Int
        get() = items.size

    fun getNextChapter(timeMs: Int): Chapter? {
        val currentTimeFinal = if (timeMs < 0) 0 else timeMs
        for (chapter in items) {
            if (chapter.startTime > currentTimeFinal) {
                return chapter
            }
        }
        return null
    }

    fun getPreviousChapter(timeMs: Int): Chapter? {
        if (items.isEmpty()) {
            return null
        }
        var foundChapter: Chapter? = null
        var lastChapter: Chapter? = null
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

//    fun getListWithState(timeMs: Int): List<Chapter> {
//        val currentChapter: Chapter? = getChapter(timeMs)
//        var foundChapter = false
//        items.forEach { chapter ->
//            if (currentChapter == null) {
//                chapter.played = false
//            } else {
//                if (currentChapter == chapter) {
//                    foundChapter = true
//                    val chapterLength = chapter.endTime - chapter.startTime
//                    if (chapterLength > 0) {
//                        chapter.progress = (timeMs - chapter.startTime).toFloat() / chapterLength
//                    }
//                } else {
//                    chapter.progress = 0.0f
//                }
//
//                chapter.played = !foundChapter
//            }
//        }
//
//        return items
//    }
}
