package au.com.shiftyjelly.pocketcasts.servers.extensions

import retrofit2.Response

fun <T> Response<T>.wasCached(): Boolean {
    return this.raw().networkResponse == null
}

fun <T> Response<T>.notModified(): Boolean {
    val networkResponse = this.raw().networkResponse
    return networkResponse != null && networkResponse.code == 304
}
