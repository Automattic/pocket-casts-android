package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil

class EpisodeSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    var searchText: EditText
    var onSearch: ((String) -> Unit)? = null
    var onFocus: (() -> Unit)? = null
    var text: String
        set(value) {
            searchText.setText(value)
            if (value.isNotEmpty()) {
                searchText.setSelection(value.length)
            }
        }
        get() { return searchText.text.toString() }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(text: Editable) {}

        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
            onSearch?.invoke(text.toString())
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_episode_search, this, true)
        searchText = findViewById(R.id.searchText)
        val cancelSearchBtn = findViewById<ImageButton>(R.id.cancelSearchBtn)
        searchText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onFocus?.invoke()
                cancelSearchBtn.show()
            } else {
                cancelSearchBtn.hide()
            }
        }
        searchText.addTextChangedListener(textChangeListener)
        cancelSearchBtn.setOnClickListener {
            searchText.clearFocus()
            searchText.setText("")
            UiUtil.hideKeyboard(searchText)
        }

        // Stops the focus search going to a detached row and causing a crash
        searchText.setOnEditorActionListener { _, _, _ ->
            UiUtil.hideKeyboard(searchText)
            true
        }
    }
}
