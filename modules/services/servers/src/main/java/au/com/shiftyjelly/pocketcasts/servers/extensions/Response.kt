package au.com.shiftyjelly.pocketcasts.servers.extensions

import retrofit2.Response

fun <T> Response<T>.wasCached(): Boolean {
    return this.raw().networkResponse == null
}
