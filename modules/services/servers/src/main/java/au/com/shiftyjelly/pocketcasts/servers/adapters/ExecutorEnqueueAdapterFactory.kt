package au.com.shiftyjelly.pocketcasts.servers.adapters

import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

/**
 * A Retrofit [CallAdapter.Factory] that ensures [Call.enqueue] runs on the provided [Executor]
 * instead of the calling thread.
 *
 * Even when using `enqueue()`, Retrofit may perform work such as `OkHttpClient` creation on the
 * calling thread, which can block the Android main thread. This adapter defers the call to
 * `enqueue()` onto the supplied [Executor] to avoid that behavior.
 *
 * This adapter only affects asynchronous execution via [Call.enqueue]. Synchronous execution
 * via [Call.execute] is not modified.
 */
internal class ExecutorEnqueueAdapterFactory(private val executor: Executor) : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<out Annotation?>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) {
            return null
        }
        require(returnType is ParameterizedType) {
            "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>"
        }
        val responseType = getParameterUpperBound(0, returnType)
        return ExecutorEnqueueAdapter<Any>(executor, retrofit.callbackExecutor(), responseType)
    }
}

private class ExecutorEnqueueAdapter<T>(
    private val executor: Executor,
    private val callbackExecutor: Executor?,
    private val responseType: Type,
) : CallAdapter<T, Call<T>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Call<T> = ExecutorEnqueueCall(executor, callbackExecutor, call)
}

private class ExecutorEnqueueCall<T>(
    private val executor: Executor,
    private val callbackExecutor: Executor?,
    private val delegate: Call<T>,
) : Call<T> {
    private val executed = AtomicBoolean(false)

    override fun enqueue(callback: Callback<T>) {
        if (executed.getAndSet(true)) {
            throw IllegalStateException("Already executed.")
        }
        executor.execute {
            delegate.enqueue(callback)
        }
    }

    override fun execute(): Response<T> {
        if (executed.getAndSet(true)) {
            throw IllegalStateException("Already executed.")
        }
        return delegate.execute()
    }

    override fun isExecuted() = executed.get()

    override fun cancel() = delegate.cancel()

    override fun isCanceled() = delegate.isCanceled

    override fun clone(): Call<T> = ExecutorEnqueueCall(executor, callbackExecutor, delegate.clone())

    override fun request(): Request = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()
}
