package au.com.shiftyjelly.pocketcasts.utils.parceler

import android.os.Parcel
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parceler

object ColorParceler : Parceler<Color?> {
    override fun create(parcel: Parcel): Color? {
        val isPresent = parcel.readByte() == 1.toByte()
        return if (isPresent) Color(parcel.readLong()) else null
    }

    override fun Color?.write(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (this != null) 1.toByte() else 0.toByte())
        if (this != null) {
            parcel.writeLong(value.toLong())
        }
    }
}
