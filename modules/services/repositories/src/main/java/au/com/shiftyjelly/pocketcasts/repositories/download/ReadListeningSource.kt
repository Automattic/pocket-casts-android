package au.com.shiftyjelly.pocketcasts.repositories.download

import okio.Buffer
import okio.Source

fun Source.withReadListener(onReadByteCount: (Long) -> Unit): Source = ReadListeningSource(this, onReadByteCount)

private class ReadListeningSource(
    private val upstream: Source,
    private val onReadByteCount: (Long) -> Unit,
) : Source {
    private var totalBytesRead = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = upstream.read(sink, byteCount)

        if (totalBytesRead == 0L && bytesRead == -1L) {
            onReadByteCount(0)
        } else if (bytesRead != -1L) {
            totalBytesRead += bytesRead
            onReadByteCount(totalBytesRead)
        }
        return bytesRead
    }

    override fun close() = upstream.close()

    override fun timeout() = upstream.timeout()
}
