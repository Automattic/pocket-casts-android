package au.com.shiftyjelly.pocketcasts.servers

import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Share
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.NoCacheTokenedOkHttpClient
import au.com.shiftyjelly.pocketcasts.servers.discover.PodcastSearch
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Single
import io.reactivex.SingleEmitter
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
open class ServerManager @Inject constructor(
    @NoCacheTokenedOkHttpClient private val httpClientNoCache: OkHttpClient,
    private val settings: Settings
) {
    companion object {
        private const val LIST_SEPERATOR = ","
        private const val NO_INTERNET_CONNECTION_MSG = "Check your connection and try again."
    }

    fun registerWithSyncServer(email: String, password: String, callback: ServerCallback<AuthResultModel>): Call? {
        return postToSyncServer("/security/register", email, password, null, true, authResultExtractionCallback(callback))
    }

    fun loginToSyncServer(email: String, password: String, callback: ServerCallback<AuthResultModel>): Call? {
        return postToSyncServer("/security/login", email, password, null, true, authResultExtractionCallback(callback))
    }

    private fun authResultExtractionCallback(callback: ServerCallback<AuthResultModel>): PostCallback {
        return object : PostCallback, ServerFailure by callback {
            override fun onSuccess(data: String?, response: ServerResponse) {
                try {
                    if (data == null) {
                        throw Exception("Response empty")
                    }
                    val jsonData = JSONObject(data)
                    val token = jsonData.getString("token")
                    val uuid = jsonData.getString("uuid")
                    callback.dataReturned(AuthResultModel(token = token, uuid = uuid))
                } catch (e: JSONException) {
                    Timber.e(e)
                    callback.dataReturned(null)
                }
            }
        }
    }

    fun obtainThirdPartyToken(email: String, password: String, scope: String, callback: ServerCallback<String>) {
        val tokenRequest = ApiTokenRequest(email, password, scope)

        val retrofit = Retrofit.Builder().baseUrl(Settings.SERVER_API_URL).addConverterFactory(MoshiConverterFactory.create()).build()
        val apiServer = retrofit.create(ApiServer::class.java)
        val call = apiServer.login(tokenRequest)
        call.enqueue(object : retrofit2.Callback<ApiTokenResponse> {
            override fun onResponse(call: retrofit2.Call<ApiTokenResponse>, response: retrofit2.Response<ApiTokenResponse>) {
                val token = response.body()?.token
                if (token != null) {
                    callback.dataReturned(token)
                } else {
                    callback.onFailed(
                        errorCode = -1,
                        userMessage = "Unable to link Pocket Casts account with Sonos.",
                        serverMessageId = null,
                        serverMessage = response.code().toString() + "Unable to link Pocket Casts account with Sonos.",
                        throwable = null
                    )
                }
            }

            override fun onFailure(call: retrofit2.Call<ApiTokenResponse>, t: Throwable) {
                callback.onFailed(
                    errorCode = -1,
                    userMessage = "Unable to link Pocket Casts account with Sonos.",
                    serverMessageId = null,
                    serverMessage = "Unable to link Pocket Casts account with Sonos. " + if (t.message == null) "" else t.message,
                    throwable = t
                )
            }
        })
    }

    fun forgottenPasswordToSyncServer(email: String, callback: ServerCallback<String>): Call? {
        return postToSyncServer("/security/forgot_password", email, null, null, true, PostCallbackDefault(callback))
    }

    fun searchForPodcasts(searchTerm: String, callback: ServerCallback<PodcastSearch>): Call? {
        if (searchTerm.isBlank()) {
            callback.dataReturned(PodcastSearch(searchTerm = searchTerm))
            return null
        }
        return postToMainServer(
            "/podcasts/search", Parameters("q", searchTerm), true,
            object : PostCallback, ServerFailure by callback {
                override fun onSuccess(data: String?, response: ServerResponse) {
                    callback.dataReturned(DataParser.parsePodcastSearch(data = data, searchTerm = searchTerm))
                }
            }
        )
    }

    suspend fun searchForPodcastsSuspend(searchTerm: String, resources: Resources): PodcastSearch {
        if (searchTerm.isEmpty()) {
            return PodcastSearch()
        }
        return suspendCancellableCoroutine { continuation ->
            searchForPodcasts(
                searchTerm = searchTerm,
                callback = object : ServerCallback<PodcastSearch> {
                    override fun dataReturned(result: PodcastSearch?) {
                        if (result == null) {
                            continuation.resumeWithException(Exception(resources.getString(R.string.error_search_failed)))
                        } else {
                            continuation.resume(result)
                        }
                    }

                    override fun onFailed(
                        errorCode: Int,
                        userMessage: String?,
                        serverMessageId: String?,
                        serverMessage: String?,
                        throwable: Throwable?,
                    ) {
                        val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, resources::getString)
                            ?: userMessage
                            ?: resources.getString(R.string.error_search_failed)
                        continuation.resumeWithException(throwable ?: Exception(message))
                    }
                }
            )
        }
    }

    open fun searchForPodcastsRx(searchTerm: String): Single<PodcastSearch> {
        return convertCallToRx { emitter ->
            searchForPodcasts(searchTerm, getRxServerCallback(emitter))
        }
    }

    fun exportFeedUrls(uuids: List<String>, callback: ServerCallback<Map<String, String>>): Call? {
        val uuidsJoined = TextUtils.join(",", uuids)
        return postToMainServer(
            "/import/export_feed_urls", Parameters("uuids", uuidsJoined), true,
            object : PostCallback, ServerFailure by callback {
                override fun onSuccess(data: String?, response: ServerResponse) {
                    callback.dataReturned(DataParser.parseExportFeedUrls(data))
                }
            }
        )
    }

    fun getSharedItemDetails(strippedUrl: String, callback: ServerCallback<Share>): Call? {
        return postToMainServer(
            strippedUrl, null, true,
            object : PostCallback, ServerFailure by callback {
                override fun onSuccess(data: String?, response: ServerResponse) {
                    callback.dataReturned(DataParser.parseShareItem(data))
                }
            }
        )
    }

    fun refreshPodcastsSync(podcasts: List<Podcast>, callback: ServerCallback<RefreshResponse>): Call? {
        if (podcasts.isEmpty()) {
            callback.dataReturned(null)
            return null
        }

        val podcastsStr = StringBuilder()
        val episodesStr = StringBuilder()

        for (i in podcasts.indices) {
            val podcast = podcasts[i]
            if (i > 0) {
                podcastsStr.append(LIST_SEPERATOR)
                episodesStr.append(LIST_SEPERATOR)
            }
            podcastsStr.append(podcast.uuid)
            episodesStr.append(podcast.latestEpisodeUuid)
        }

        val parameters = Parameters()
            .add("podcasts", podcastsStr.toString())
            .add("last_episodes", episodesStr.toString())
            .add("push_on", "false")

        return postToMainServer(
            "/user/update", parameters, false,
            object : PostCallback, ServerFailure by callback {
                override fun onSuccess(data: String?, response: ServerResponse) {
                    callback.dataReturned(DataParser.parseRefreshPodcasts(data))
                }
            }
        )
    }

    private fun <T> getRxServerCallback(emitter: SingleEmitter<T>): ServerCallback<T> {
        return object : ServerCallback<T> {

            override fun dataReturned(result: T?) {
                if (emitter.isDisposed) return
                emitter.onSuccess(result!!)
            }

            override fun onFailed(
                errorCode: Int,
                userMessage: String?,
                serverMessageId: String?,
                serverMessage: String?,
                throwable: Throwable?
            ) {
                if (emitter.isDisposed) return
                emitter.onError(throwable ?: UnknownError())
            }
        }
    }

    private fun <T> convertCallToRx(call: (emiiter: SingleEmitter<T>) -> Call?): Single<T> {
        return Single.create {
            try {
                val task = call(it)
                it.setCancellable { task?.cancel() }
            } catch (e: Exception) {
                it.onError(e)
            }
        }
    }

    private fun addDeviceParameters(parameters: Parameters) {
        parameters
            .add("v", Settings.PARSER_VERSION)
            .add("av", settings.getVersion())
            .add("ac", "" + settings.getVersionCode())
            .add("dt", "2")
            .add("c", Locale.getDefault().country)
            .add("l", Locale.getDefault().language)
            .add("m", Build.MODEL)
            .add("scope", "mobile")
    }

    private fun postToSyncServer(servicePath: String, email: String?, password: String?, parameters: Parameters?, async: Boolean, postCallback: PostCallback): Call? {
        val params = parameters ?: Parameters()
        params.add("email", email)
        password?.let { params.add("password", it) }
        addDeviceParameters(params)
        return post(Settings.SERVER_API_URL, servicePath, params, async, postCallback)
    }

    private fun postToMainServer(servicePath: String, parameters: Parameters?, async: Boolean, postCallback: PostCallback): Call? {
        val parametersOrEmpty = parameters ?: Parameters()
        addDeviceParameters(parametersOrEmpty)
        return post(Settings.SERVER_MAIN_URL, servicePath, parametersOrEmpty, async, postCallback)
    }

    private fun post(serverUrl: String, servicePath: String, parameters: Parameters, async: Boolean, callback: PostCallback): Call? {
        return post(serverUrl, servicePath, parameters, async, 1, callback)
    }

    private fun post(serverUrl: String, servicePath: String, parameters: Parameters, async: Boolean, pollCount: Int, callback: PostCallback): Call? {
        val url = serverUrl + servicePath

        val formBody = parameters.toFormBody()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Settings.USER_AGENT_POCKETCASTS_SERVER)
            .post(formBody)
            .build()

        // if we were called from the UI thread then call back on this so we can change UI elements
        val callbackOnUiThread = async && Looper.myLooper() == Looper.getMainLooper()

        val requestCallback = object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val responseDebug = String.format("Post response %d %s", response.code, call.request().url)
                val serverResponse = DataParser.parseServerResponse(body)
                if (serverResponse.requiresPolling()) {
                    val nextPollCount = pollCount + 1
                    if (nextPollCount > 6) {
                        if (callbackOnUiThread) {
                            Handler(Looper.getMainLooper()).post {
                                callback.onFailed(
                                    errorCode = -1,
                                    userMessage = response.message,
                                    serverMessageId = null,
                                    serverMessage = responseDebug,
                                    throwable = null
                                )
                            }
                        } else {
                            callback.onFailed(
                                errorCode = -1,
                                userMessage = response.message,
                                serverMessageId = null,
                                serverMessage = responseDebug,
                                throwable = null
                            )
                        }
                    } else {
                        try {
                            val time = if (pollCount > 4) 10000 else 5000
                            Thread.sleep(time.toLong())
                        } catch (e: InterruptedException) {
                            // ignore
                        }

                        if (callbackOnUiThread) {
                            Handler(Looper.getMainLooper()).post { post(serverUrl, servicePath, parameters, async, nextPollCount, callback) }
                        } else {
                            post(serverUrl, servicePath, parameters, async, nextPollCount, callback)
                        }
                    }
                } else {
                    if (callbackOnUiThread) {
                        Handler(Looper.getMainLooper()).post {
                            if (serverResponse.success) {
                                callback.onSuccess(serverResponse.data, serverResponse)
                            } else {
                                callback.onFailed(
                                    errorCode = serverResponse.errorCode,
                                    userMessage = if (serverResponse.message == null) NO_INTERNET_CONNECTION_MSG else serverResponse.message,
                                    serverMessageId = serverResponse.serverMessageId,
                                    serverMessage = responseDebug,
                                    throwable = null
                                )
                            }
                        }
                    } else {
                        if (serverResponse.success) {
                            callback.onSuccess(serverResponse.data, serverResponse)
                        } else {
                            callback.onFailed(
                                errorCode = serverResponse.errorCode,
                                userMessage = if (serverResponse.message == null) NO_INTERNET_CONNECTION_MSG else serverResponse.message,
                                serverMessageId = serverResponse.serverMessageId,
                                serverMessage = responseDebug,
                                throwable = null
                            )
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "Post response failed.")
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Response failed from ${call.request().url}.")
                if (callbackOnUiThread) {
                    Handler(Looper.getMainLooper()).post {
                        callback.onFailed(
                            errorCode = -1,
                            userMessage = NO_INTERNET_CONNECTION_MSG,
                            serverMessageId = null,
                            serverMessage = e.message,
                            throwable = e
                        )
                    }
                } else {
                    callback.onFailed(
                        errorCode = -1,
                        userMessage = NO_INTERNET_CONNECTION_MSG,
                        serverMessageId = null,
                        serverMessage = e.message,
                        throwable = e
                    )
                }
            }
        }

        val call = httpClientNoCache.newCall(request)
        if (async) {
            call.enqueue(requestCallback)
            return call
        } else {
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    requestCallback.onResponse(call, response)
                } else {
                    requestCallback.onFailure(call, IOException("Unexpected code $response"))
                }
            } catch (e: IOException) {
                Timber.e(e)
                requestCallback.onFailure(call, e)
                return null
            }

            return null
        }
    }

    private inner class Parameters {
        private val pairs = mutableListOf<Pair<String, String?>>()

        constructor()

        constructor(name: String, value: String) {
            add(name, value)
        }

        fun add(name: String, value: String?): Parameters {
            pairs.add(Pair(name, value))
            return this
        }

        fun toFormBody(): FormBody {
            val builder = FormBody.Builder()
            for (pair in pairs) {
                val first = pair.first
                val second = pair.second
                if (second != null) {
                    builder.add(first, second)
                }
            }
            return builder.build()
        }

        override fun toString(): String {
            return pairs.joinToString { pair -> "${pair.first}=${pair.second}" }
        }
    }
}
