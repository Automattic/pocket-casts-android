package au.com.shiftyjelly.pocketcasts.servers.sync

import androidx.annotation.NonNull
import io.reactivex.FlowableEmitter
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.File
import java.io.IOException

class ProgressRequestBody(private val delegate: RequestBody, private val listener: Listener) : RequestBody() {
    companion object {
        fun create(contentType: MediaType?, file: File, emitter: FlowableEmitter<Float>): ProgressRequestBody {
            val requestBody = file.asRequestBody(contentType)
            return ProgressRequestBody(
                requestBody,
                object : Listener {
                    override fun onRequestProgress(bytesWritten: Long, contentLength: Long) {
                        val progress = bytesWritten.toFloat() / contentLength.toFloat()
                        emitter.onNext(progress)
                    }
                }
            )
        }
    }
    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun contentLength(): Long {
        try {
            return delegate.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(@NonNull sink: BufferedSink) {
        val bufferedSink = CountingSink(sink).buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    internal inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {

        private var bytesWritten: Long = 0

        @Throws(IOException::class)
        override fun write(@NonNull source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onRequestProgress(bytesWritten, contentLength())
        }
    }

    interface Listener {
        fun onRequestProgress(bytesWritten: Long, contentLength: Long)
    }
}
