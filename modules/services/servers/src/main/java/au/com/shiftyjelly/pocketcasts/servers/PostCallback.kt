package au.com.shiftyjelly.pocketcasts.servers

interface PostCallback : ServerFailure {
    fun onSuccess(data: String?, response: ServerResponse)
}
