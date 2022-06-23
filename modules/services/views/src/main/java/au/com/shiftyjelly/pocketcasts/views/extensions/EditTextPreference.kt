package au.com.shiftyjelly.pocketcasts.views.extensions

import android.text.InputFilter
import android.text.InputType
import androidx.preference.EditTextPreference

fun EditTextPreference.setInputAsSeconds() {
    setOnBindEditTextListener { editText ->
        with(editText) {
            inputType = InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
            // limits to 99,999 seconds, about 28 hours
            filters = arrayOf(InputFilter.LengthFilter(5))
            selectAll()
        }
    }
}
