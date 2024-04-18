package au.com.shiftyjelly.pocketcasts.utils.parceler

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object DurationParceler : Parceler<Duration?> {
    override fun create(parcel: Parcel) =
        if (parcel.dataSize() > 0) {
            parcel.readLong().seconds
        } else {
            null
        }

    override fun Duration?.write(parcel: Parcel, flags: Int) {
        if (this != null) {
            parcel.writeLong(inWholeSeconds)
        }
    }
}
