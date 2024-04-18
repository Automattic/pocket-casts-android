package au.com.shiftyjelly.pocketcasts.utils.parceler

import android.os.Parcel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.parcelize.Parceler

object DurationParceler : Parceler<Duration?> {
    override fun create(parcel: Parcel): Duration? {
        val isPresent = parcel.readByte() == 0.toByte()
        return if (isPresent) parcel.readLong().milliseconds else null
    }

    override fun Duration?.write(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (this != null) 1.toByte() else 0.toByte())
        if (this != null) {
            parcel.writeLong(inWholeMilliseconds)
        }
    }
}
