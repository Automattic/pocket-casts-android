package au.com.shiftyjelly.pocketcasts.servers

import android.content.Context
import android.os.Build
import android.text.TextUtils
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Share
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.NoCacheTokened
import au.com.shiftyjelly.pocketcasts.servers.discover.PodcastSearch
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshPodcastBatcher
import au.com.shiftyjelly.pocketcasts.utils.extensions.await
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.rx2.rxSingle
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
open class ServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @NoCacheTokened private val httpClientNoCache: OkHttpClient,
    private val settings: Settings,
) {
    companion object {
        private const val LIST_SEPARATOR = ","
        private const val NO_INTERNET_CONNECTION_MSG = "Check your connection and try again."
    }

    suspend fun searchForPodcasts(searchTerm: String): Result<PodcastSearch> {
        return if (searchTerm.isBlank()) {
            Result.success(PodcastSearch(searchTerm = searchTerm))
        } else {
            postToMainServer("/podcasts/search", Parameters("q", searchTerm)).map { response ->
                DataParser.parsePodcastSearch(response.data, searchTerm)
            }
        }
    }

    fun searchForPodcastsRx(searchTerm: String): Single<PodcastSearch> {
        return rxSingle { searchForPodcasts(searchTerm).getOrThrow() }
    }

    suspend fun exportFeedUrls(uuids: List<String>): Result<Map<String, String>?> {
        val uuidsJoined = TextUtils.join(LIST_SEPARATOR, uuids)
        return postToMainServer("/import/export_feed_urls", Parameters("uuids", uuidsJoined)).map { response ->
            DataParser.parseExportFeedUrls(response.data)
        }
    }

    suspend fun getSharedItemDetails(strippedUrl: String): Result<Share?> {
        return postToMainServer(strippedUrl).map { response ->
            DataParser.parseShareItem(response.data)
        }
    }

    suspend fun refreshPodcastsSync(podcasts: List<Podcast>): Result<RefreshResponse?> {
        val batchSize = settings.getRefreshPodcastsBatchSize().coerceIn(100L, Int.MAX_VALUE.toLong()).toInt()
        val batcher = RefreshPodcastBatcher(batchSize)
        return batcher.refreshPodcasts(podcasts) { parameters ->
            val response = postToMainServer("/user/update", parameters).getOrThrow()
            DataParser.parseRefreshPodcasts(response.data)
        }
    }

    private suspend fun postToMainServer(path: String, parameters: Parameters? = null, attemptCount: Int = 1): Result<ServerResponse> {
        return try {
            val requestParams = parameters ?: Parameters()
            addDeviceParameters(requestParams)
            val request = Request.Builder()
                .url(Settings.SERVER_MAIN_URL + path)
                .post(requestParams.toFormBody())
                .build()

            val response = httpClientNoCache.newCall(request).await()
            val serverResponse = DataParser.parseServerResponse(response.body.string())

            when {
                serverResponse.requiresPolling() -> {
                    val nextAttemptCount = attemptCount + 1
                    if (nextAttemptCount > 6) {
                        Result.failure(IOException(serverResponse.message))
                    } else {
                        delay(if (attemptCount > 4) 10.seconds else 5.seconds)
                        postToMainServer(path, parameters, nextAttemptCount)
                    }
                }

                serverResponse.success -> {
                    Result.success(serverResponse)
                }

                else -> {
                    val resources = context.resources
                    val message = LocaliseHelper.serverMessageIdToMessage(serverResponse.serverMessageId, resources::getString)
                        ?: serverResponse.message
                        ?: NO_INTERNET_CONNECTION_MSG
                    Result.failure(IOException(message))
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            Result.failure(IOException(NO_INTERNET_CONNECTION_MSG, e))
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

    internal class Parameters {
        private val pairs = mutableListOf<Pair<String, String?>>()

        constructor()

        constructor(name: String, value: String) {
            add(name, value)
        }

        fun add(name: String, value: String?): Parameters {
            pairs.add(Pair(name, value))
            return this
        }

        operator fun get(key: String) = pairs.associate { (first, _) ->
            first to pairs.filter { it.first == first }.map { it.second }
        }[key]?.joinToString(LIST_SEPARATOR)

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
