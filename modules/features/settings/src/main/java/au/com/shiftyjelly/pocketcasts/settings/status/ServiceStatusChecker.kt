package au.com.shiftyjelly.pocketcasts.settings.status

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.utils.extensions.await
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ServiceStatusChecker @Inject constructor(@ApplicationContext val context: Context) {

    suspend fun check(check: Check): ServiceStatus {
        return when (check) {
            is Check.Internet -> checkInternet()
            is Check.Urls -> {
                var status: ServiceStatus = ServiceStatus.Failed(userMessage = null, log = "No urls checked")
                for (url in check.urls) {
                    status = checkUrl(url = url)
                    if (status is ServiceStatus.Failed) {
                        return status
                    }
                }
                return status
            }
        }
    }

    private suspend fun checkUrl(url: String): ServiceStatus {
        val log = StringBuilder()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val request: Request = chain.request()
                val startTime = System.nanoTime()
                log.append(String.format("Sending request %s on %s%n%s", request.url, chain.connection(), request.headers))
                val response: Response = chain.proceed(request)
                val endTime = System.nanoTime()
                log.append(String.format("Received response for %s in %.1fms%n%s", response.request.url, (endTime - startTime) / 1e6, response.headers))
                response
            }
            .build()

        try {
            val request = Request.Builder()
                .url(url)
                .build()
            val response = okHttpClient.newCall(request).await()
            return if (response.isSuccessful) {
                ServiceStatus.Success
            } else {
                ServiceStatus.Failed(userMessage = null, log.toString())
            }
        } catch (ex: Exception) {
            if (log.isNotEmpty()) {
                log.append("\n")
            }
            log.append("Exception: ").append(ex.stackTraceToString())
            return ServiceStatus.Failed(userMessage = ex.message, log = log.toString())
        }
    }

    private fun checkInternet(): ServiceStatus {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return ServiceStatus.Failed(userMessage = null, log = "Unable to get network capabilities")
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                ServiceStatus.Success
            } else {
                ServiceStatus.Failed(userMessage = null, log = capabilities.toString())
            }
        } catch (ex: Exception) {
            ServiceStatus.Failed(userMessage = null, log = ex.stackTraceToString())
        }
    }

    sealed class Check {
        object Internet : Check() {
            override fun toString(): String {
                return "Internet Check"
            }
        }
        data class Urls(val urls: List<String>) : Check() {
            override fun toString(): String {
                return "Urls Check ${urls.joinToString(", ")}"
            }
        }
    }
}
