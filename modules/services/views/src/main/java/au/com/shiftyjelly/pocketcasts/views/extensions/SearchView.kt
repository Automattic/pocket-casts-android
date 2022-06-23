package au.com.shiftyjelly.pocketcasts.views.extensions

import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView

fun findEditText(parent: ViewGroup): EditText? {
    for (i in 0 until parent.childCount) {
        val child = parent.getChildAt(i)
        if (child is EditText) {
            return child
        } else if (child is ViewGroup) {
            return findEditText(child)
        }
    }
    return null
}

val SearchView.editText: EditText?
    get() {
        return findEditText(this)
    }

fun SearchView.showKeyboard() {
    editText?.showKeyboard()
}
