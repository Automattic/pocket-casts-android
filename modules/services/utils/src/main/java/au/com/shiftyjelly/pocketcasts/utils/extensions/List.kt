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

fun <T1, T2, R> List<T1>.equalsBy(
    other: List<T2>,
    leftSelector: (T1) -> R,
    rightSelector: (T2) -> R,
): Boolean {
    if (size != other.size) {
        return false
    }

    for (index in indices) {
        if (leftSelector(this[index]) != rightSelector(other[index])) {
            return false
        }
    }

    return true
}
