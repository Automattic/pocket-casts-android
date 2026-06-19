package au.com.shiftyjelly.pocketcasts.views.extensions

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowInsets
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
    post {
        requestFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = windowInsetsController
            if (controller != null) {
                controller.show(WindowInsets.Type.ime())
            } else {
                // The view isn't attached to a window yet (e.g. called from onViewCreated),
                // so windowInsetsController is null. Retry once the view is attached.
                post { windowInsetsController?.show(WindowInsets.Type.ime()) }
            }
        } else {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.showSoftInput(this, 0)
        }
    }
}
