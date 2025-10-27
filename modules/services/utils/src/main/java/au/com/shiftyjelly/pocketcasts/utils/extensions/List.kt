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

fun <T, R> List<T>.containsAllInOrderBy(
    other: List<T>,
    selector: (T) -> R,
): Boolean {
    if (other.size > size) {
        return false
    }

    for (index in other.indices) {
        if (selector(this[index]) != selector(other[index])) {
            return false
        }
    }

    return true
}
