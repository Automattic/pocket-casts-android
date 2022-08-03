package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.widget.TextView
import androidx.annotation.StringRes

fun TextView.setTextSafe(@StringRes stringRes: Int?) {
    if (stringRes == null) {
        text = stringRes
    } else {
        setText(stringRes)
    }
}
