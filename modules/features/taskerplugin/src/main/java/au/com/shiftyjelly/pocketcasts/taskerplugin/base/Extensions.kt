package au.com.shiftyjelly.pocketcasts.taskerplugin.base

val String?.nullIfEmpty get() = if (isNullOrEmpty()) null else this
fun <T> tryOrNull(handleError: ((Throwable) -> T?)? = null, block: () -> T?): T? = try {
    block()
} catch (t: Throwable) {
    handleError?.invoke(t)
}
