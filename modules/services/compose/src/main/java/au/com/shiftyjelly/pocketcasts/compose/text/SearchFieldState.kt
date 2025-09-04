package au.com.shiftyjelly.pocketcasts.compose.text

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Stable
class SearchFieldState(
    val textState: TextFieldState,
) {
    constructor(
        initialText: String = "",
        initialSelection: TextRange = TextRange(initialText.length),
    ) : this(TextFieldState(initialText, initialSelection))

    @OptIn(FlowPreview::class)
    val textFlow = snapshotFlow { textState.text.toString().trim() }
        .distinctUntilChanged()
        .debounce { searchTerm -> if (searchTerm.isEmpty()) 0 else 300 }
}

@Composable
fun rememberSearchFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length),
): SearchFieldState = SearchFieldState(rememberTextFieldState(initialText, initialSelection))
