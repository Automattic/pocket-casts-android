package au.com.shiftyjelly.pocketcasts.servers

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.OkHttpClient
import okhttp3.Response

sealed interface OkHttpInterceptor {
    @Throws(IOException::class)
    fun intercept(chain: Chain): Response
}

fun interface ClientInterceptor : OkHttpInterceptor

fun Interceptor.toClientInterceptor() = ClientInterceptor { chain -> intercept(chain) }

fun interface NetworkInterceptor : OkHttpInterceptor

fun Interceptor.toNetworkInterceptor() = NetworkInterceptor { chain -> intercept(chain) }

fun OkHttpClient.Builder.addInterceptors(interceptors: List<OkHttpInterceptor>) = interceptors.fold(this) { builder, okHttpInterceptor ->
    when (okHttpInterceptor) {
        is ClientInterceptor -> builder.addInterceptor(Interceptor { okHttpInterceptor.intercept(it) })
        is NetworkInterceptor -> builder.addNetworkInterceptor(Interceptor { okHttpInterceptor.intercept(it) })
    }
}
