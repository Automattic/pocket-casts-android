package au.com.shiftyjelly.pocketcasts.servers

import android.os.Handler
import android.os.Looper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.ShowNotesCacheCallFactory
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Completable
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class ServerShowNotesManager @Inject constructor(
    @ShowNotesCacheCallFactory private val showNotesCacheCallFactory: Call.Factory,
) {

    fun cacheShowNotes(episodeUuid: String): Completable {
        return Completable.fromAction {
            val url = buildUrl(episodeUuid)
            val requestCache = buildCachedShowNotesRequest(url)
            showNotesCacheCallFactory.newCall(requestCache).execute().use { cachedResponse ->
                val cachedNotes = getShowNotesFromResponse(cachedResponse)
                // only download if not cached already
                if (cachedNotes.isNullOrEmpty()) {
                    // download to cache
                    val request = buildNetworkShowNotesRequest(url)
                    showNotesCacheCallFactory.newCall(request).execute().use { response ->
                        Timber.i("Show notes cached %s %s", episodeUuid, if (response.isSuccessful) "Successful" else "Failed")
                    }
                }
            }
        }.doOnError { throwable ->
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "Unable to cache show notes.")
            Timber.e(throwable)
        }
    }

    fun loadShowNotes(episodeUuid: String, callback: CachedServerCallback<String>?) {
        val url = buildUrl(episodeUuid)

        val uiHandler = if (callback == null) null else Handler(Looper.getMainLooper())

        // load the cache version first for speed
        loadCachedShowNotes(
            url,
            object : ShowNotesListener {
                override fun complete(showNotes: String?) {
                    if (showNotes != null && showNotes.isNotBlank()) {
                        uiHandler?.post { callback?.cachedDataFound(showNotes) }
                        return
                    }
                    // check the server for an updated version
                    loadNetworkShowNotes(url, showNotes, uiHandler, callback)
                }
            }
        )
    }

    suspend fun loadShowNotes(episodeUuid: String): String? =
        suspendCoroutine { cont ->
            loadShowNotes(
                episodeUuid,
                object : CachedServerCallback<String> {
                    override fun cachedDataFound(data: String) {
                        cont.resume(data)
                    }

                    override fun networkDataFound(data: String) {
                        cont.resume(data)
                    }

                    override fun notFound() {
                        cont.resume(null)
                    }
                }
            )
        }

    private fun buildUrl(episodeUuid: String): String {
        return Settings.SERVER_CACHE_URL + "/mobile/episode/show_notes/" + episodeUuid
    }

    private fun loadCachedShowNotes(url: String, listener: ShowNotesListener) {
        val requestCache = buildCachedShowNotesRequest(url)

        showNotesCacheCallFactory.newCall(requestCache).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e)
                listener.complete(null)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                // callback if cached version found
                if (response.code != 504) {
                    listener.complete(getShowNotesFromResponse(response))
                } else {
                    listener.complete(null)
                }
            }
        })
    }

    private fun loadNetworkShowNotes(url: String, cachedNotes: String?, uiHandler: Handler?, callback: CachedServerCallback<String>?) {
        val request = buildNetworkShowNotesRequest(url)
        showNotesCacheCallFactory.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e)
                if (cachedNotes.isNullOrBlank()) {
                    uiHandler?.post { callback?.notFound() }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val networkNotes = getShowNotesFromResponse(response)
                    if (networkNotes != null && networkNotes.isNotBlank() && networkNotes != cachedNotes) {
                        uiHandler?.post { callback?.networkDataFound(networkNotes) }
                    } else {
                        uiHandler?.post { callback?.notFound() }
                    }
                } else if (cachedNotes.isNullOrBlank()) {
                    uiHandler?.post { callback?.notFound() }
                }
            }
        })
    }

    private fun buildNetworkShowNotesRequest(url: String): Request {
        return Request.Builder()
            .cacheControl(CacheControl.FORCE_NETWORK)
            .url(url)
            .build()
    }

    private fun buildCachedShowNotesRequest(url: String): Request {
        return Request.Builder()
            .cacheControl(CacheControl.Builder().onlyIfCached().build())
            .url(url)
            .build()
    }

    internal interface ShowNotesListener {
        fun complete(showNotes: String?)
    }

    private fun getShowNotesFromResponse(response: Response): String? {
        return try {
            val json = JSONObject(response.body?.string() ?: "")
            json.getString("show_notes")
        } catch (e: Exception) {
            null
        }
    }
}
