package au.com.shiftyjelly.pocketcasts.utils.extensions

fun <T> List<T>.padEnd(
    length: Int,
    padItem: (List<T>, Int) -> T = { list, newIndex -> list[newIndex % list.size] },
): List<T> {
    return if (size < length) {
        buildList(length) {
            addAll(this@padEnd)
            repeat(length - this@padEnd.size) { index ->
                val newIndex = index + this@padEnd.size
                add(padItem(this@padEnd, newIndex))
            }
        }
    } else {
        this
    }
}
