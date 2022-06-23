package au.com.shiftyjelly.pocketcasts.views.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.addAfterTextChanged(onChange: (text: String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(text: Editable?) {
            onChange(text.toString())
        }

        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.addOnTextChanged(onChange: (text: String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(text: Editable?) {}

        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
            onChange(text.toString())
        }
    })
}

fun EditText.showKeyboard() {
    requestFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}
