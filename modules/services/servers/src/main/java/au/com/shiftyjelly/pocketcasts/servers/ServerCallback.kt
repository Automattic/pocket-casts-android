package au.com.shiftyjelly.pocketcasts.servers

interface ServerCallback<T> : ServerFailure {
    fun dataReturned(result: T?)
}
