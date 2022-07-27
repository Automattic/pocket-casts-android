package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable

fun LazyGridScope.header(content: @Composable LazyGridItemScope.() -> Unit) {
    item(span = { GridItemSpan(this.maxCurrentLineSpan) }, content = content)
}
