package au.com.shiftyjelly.pocketcasts.servers

interface CachedServerCallback<T> {

    fun cachedDataFound(data: T)
    fun networkDataFound(data: T)
    fun notFound()
}
