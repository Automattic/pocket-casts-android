package au.com.shiftyjelly.pocketcasts.servers

import android.os.Handler
import android.os.Looper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.NoCacheOkHttpClient
import au.com.shiftyjelly.pocketcasts.utils.EncodingHelper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ShareServerManagerImpl @Inject constructor(@NoCacheOkHttpClient private val httpClient: OkHttpClient) : ShareServerManager {

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

        fun buildSecurityHash(date: String): String? {
            val stringToHash = date + Settings.SHARING_SERVER_SECRET
            return EncodingHelper.SHA1(stringToHash)
        }

        @Throws(JSONException::class)
        fun sharePodcastListToJson(title: String, description: String?, podcasts: List<Podcast>, date: String, hash: String?): String {
            val json = JSONObject()
            json.put("title", title)
            if (description != null) {
                json.put("description", description)
            }
            val podcastsJson = JSONArray()
            for (podcast in podcasts) {
                val podcastJson = JSONObject()
                podcastJson.put("uuid", podcast.uuid)
                podcastJson.put("title", podcast.title)
                podcastJson.put("author", podcast.author)
                podcastsJson.put(podcastJson)
            }
            json.put("podcasts", podcastsJson)
            json.put("datetime", date)
            json.put("h", hash)
            return json.toString()
        }

        fun parseCreatePodcastListResponse(response: String?): String? {
            if (response == null) {
                return null
            }
            try {
                val json = JSONObject(response)
                val status = json.optString("status", "")
                if (status.isEmpty() || status != "ok") {
                    return null
                }
                val resultJson = json.optJSONObject("result")
                val url = resultJson?.optString("share_url", "")
                return if (url.isNullOrEmpty()) null else url
            } catch (e: Exception) {
                Timber.e(e)
            }

            return null
        }

        fun parseLoadPodcastListResponse(jsonResponse: String): PodcastListResponse? {
            try {
                val json = JSONObject(jsonResponse)
                val response = PodcastListResponse()
                response.title = DataParser.getString(json, "title")
                response.description = DataParser.getString(json, "description")
                val podcastsJson = json.optJSONArray("podcasts")
                if (podcastsJson != null) {
                    for (i in 0 until podcastsJson.length()) {
                        val podcastJson = podcastsJson.getJSONObject(i)
                        val uuid = DataParser.getString(podcastJson, "uuid") ?: continue
                        val podcast = Podcast(
                            uuid = uuid,
                            title = DataParser.getString(podcastJson, "title") ?: "",
                            author = DataParser.getString(podcastJson, "author") ?: ""
                        )
                        response.addPodcastHeader(podcast)
                    }
                }
                return response
            } catch (e: Exception) {
                Timber.e(e)
            }

            return null
        }

        fun extractShareListIdFromWebUrl(id: String?): String? {
            val host = Settings.SERVER_LIST_HOST
            return id?.replace("https://$host/", "")?.replace("http://$host/", "")?.replace("/$host/", "")?.replace(".html", "")
        }
    }

    override fun sharePodcastList(title: String, description: String, podcasts: List<Podcast>, callback: ShareServerManager.SendPodcastListCallback) {
        try {
            val date = DATE_FORMAT.format(Date())
            val hash = buildSecurityHash(date)
            val json = sharePodcastListToJson(title, description, podcasts, date, hash)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder().url(Settings.SERVER_SHARING_URL + "/share/list").post(body).build()
            val call = httpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.e(e)
                    Handler(Looper.getMainLooper()).post { callback.onFailed() }
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    val url = parseCreatePodcastListResponse(responseBody)
                    Handler(Looper.getMainLooper()).post {
                        if (url == null || url.isBlank()) {
                            callback.onFailed()
                        } else {
                            callback.onSuccess(url)
                        }
                    }
                }
            })
        } catch (e: JSONException) {
            Timber.e(e)
            callback.onFailed()
        }
    }

    override fun loadPodcastList(id: String, callback: ShareServerManager.PodcastListCallback) {
        val listId = id.trimStart('/')
        val jsonUrl = String.format("%s/%s.json", Settings.SERVER_LIST_URL, listId)
        val request = Request.Builder().url(jsonUrl).build()
        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e)
                Handler(Looper.getMainLooper()).post { callback.onFailed() }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                val listResponse = parseLoadPodcastListResponse(body)

                Handler(Looper.getMainLooper()).post {
                    if (listResponse == null) {
                        callback.onFailed()
                    } else {
                        callback.onSuccess(listResponse.title, listResponse.description, listResponse.podcasts)
                    }
                }
            }
        })
    }

    override fun extractShareListIdFromWebUrl(webUrl: String?): String? {
        return ShareServerManagerImpl.extractShareListIdFromWebUrl(webUrl)
    }

    data class PodcastListResponse(
        var title: String? = null,
        var description: String? = null,
        var podcasts: MutableList<Podcast> = mutableListOf()
    ) {

        fun addPodcastHeader(podcast: Podcast) {
            podcasts.add(podcast)
        }
    }
}
