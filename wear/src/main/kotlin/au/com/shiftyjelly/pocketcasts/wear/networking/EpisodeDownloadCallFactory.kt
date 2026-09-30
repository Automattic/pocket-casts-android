package au.com.shiftyjelly.pocketcasts.wear.networking

import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloadRequest
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.okhttp.impl.RequestTypeHolder.Companion.requestType
import com.google.android.horologist.networks.okhttp.requestTypeOrNull
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response

class EpisodeDownloadCallFactory(
    private val delegate: Call.Factory,
) : Call.Factory {
    override fun newCall(request: Request): Call {
        val downloadRequest = request.tag(EpisodeDownloadRequest::class.java)
        return if (downloadRequest != null && request.requestTypeOrNull == null) {
            val requestType = if (downloadRequest.waitForWifi) WifiOnlyDownloadRequest else RequestType.MediaRequest.DownloadRequest
            CancellableCall(delegate.newCall(request.newBuilder().requestType(requestType).build()))
        } else {
            delegate.newCall(request)
        }
    }

    private class CancellableCall(
        private val delegate: Call,
    ) : Call by delegate {
        private val isCompleted = AtomicBoolean()

        @Volatile
        private var isCancelled = false

        @Volatile
        private var callback: Callback? = null

        override fun enqueue(responseCallback: Callback) {
            callback = responseCallback
            if (isCancelled) {
                failCancelled()
                return
            }
            delegate.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (isCompleted.compareAndSet(false, true)) {
                            responseCallback.onFailure(this@CancellableCall, e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (isCompleted.compareAndSet(false, true)) {
                            responseCallback.onResponse(this@CancellableCall, response)
                        } else {
                            response.close()
                        }
                    }
                },
            )
        }

        override fun cancel() {
            isCancelled = true
            delegate.cancel()
            failCancelled()
        }

        private fun failCancelled() {
            val callback = callback
            if (callback != null && isCompleted.compareAndSet(false, true)) {
                callback.onFailure(this, IOException("Canceled"))
            }
        }

        override fun clone(): Call = CancellableCall(delegate.clone())
    }
}
