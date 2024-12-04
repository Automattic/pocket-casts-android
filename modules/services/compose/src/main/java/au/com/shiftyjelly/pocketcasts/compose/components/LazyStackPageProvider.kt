@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density

@DslMarker
annotation class LazyStackDslMarker

@Stable
@LazyStackDslMarker
interface LazyStackScope {
    fun page(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyStackPageScope.() -> Unit,
    )

    fun pages(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        pageContent: @Composable LazyStackPageScope.(index: Int) -> Unit,
    )
}

fun <T> LazyStackScope.pages(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    pageContent: @Composable LazyStackPageScope.(item: T) -> Unit,
) = pages(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) },
    pageContent = { index: Int -> pageContent(items[index]) },
)

@Stable
@LazyStackDslMarker
class LazyStackPageScope internal constructor() {
    fun Modifier.swipeablePage(
        directions: Set<SwipeDirection> = DefaultSwipeDirections,
        onSwipedIn: ((SwipeDirection) -> Unit)? = null,
        onSwipedOut: ((SwipeDirection) -> Unit)? = null,
    ) = this then SwipeableModifier(directions, onSwipedIn, onSwipedOut)
}

private val DefaultSwipeDirections = setOf(SwipeDirection.Left, SwipeDirection.Right)

@Composable
internal fun rememberLazyStackPageProviderLambda(
    state: LazyStackState,
    content: LazyStackScope.() -> Unit,
): () -> LazyStackPageProvider {
    val latestContent by rememberUpdatedState(content)
    return remember(state) {
        val scope = LazyStackPageScope()
        val intervalContentState = derivedStateOf(referentialEqualityPolicy()) {
            LazyStackIntervalContent(latestContent)
        }
        val pageProvider = derivedStateOf(referentialEqualityPolicy()) {
            val intervalContent = intervalContentState.value
            LazyStackPageProvider(
                intervalContent = intervalContent,
                pageScope = scope,
                map = KeyIndexMap(
                    index = state.currentPageIndex,
                    size = 10,
                    intervalContent = intervalContent,
                ),
            )
        }
        pageProvider::value
    }
}

internal class LazyStackPageProvider(
    private val intervalContent: LazyStackIntervalContent,
    private val pageScope: LazyStackPageScope,
    val map: KeyIndexMap,
) : LazyLayoutItemProvider {
    override val itemCount get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        intervalContent.withInterval(index) { localIndex, content ->
            content.page(pageScope, localIndex)
        }
    }

    override fun getKey(index: Int) = map.getKey(index) ?: intervalContent.getKey(index)

    override fun getIndex(key: Any) = map.getIndex(key)

    override fun getContentType(index: Int) = intervalContent.getContentType(index)

    fun findIndexByKey(key: Any?, lastKnownIndex: Int): Int {
        return when {
            key == null || itemCount == 0 -> lastKnownIndex
            lastKnownIndex < itemCount && key == getKey(lastKnownIndex) -> lastKnownIndex
            else -> getIndex(key).takeIf { it != -1 } ?: lastKnownIndex
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyStackPageProvider) return false
        if (intervalContent != other.intervalContent) return false
        return true
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}

internal class LazyStackIntervalContent(
    content: LazyStackScope.() -> Unit,
) : LazyLayoutIntervalContent<SwipableStackPageInterval>(), LazyStackScope {
    override val intervals = MutableIntervalList<SwipableStackPageInterval>()

    init {
        apply(content)
    }

    override fun page(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyStackPageScope.() -> Unit,
    ) {
        intervals.addInterval(
            size = 1,
            SwipableStackPageInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                page = { content() },
            ),
        )
    }

    override fun pages(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        pageContent:
        @Composable()
        (LazyStackPageScope.(index: Int) -> Unit),
    ) {
        intervals.addInterval(
            count,
            SwipableStackPageInterval(
                key = key,
                type = contentType,
                page = pageContent,
            ),
        )
    }
}

internal class SwipableStackPageInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val page: @Composable LazyStackPageScope.(index: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval

internal data class SwipeableModifier(
    val directions: Set<SwipeDirection>,
    val onSwipedIn: ((SwipeDirection) -> Unit)?,
    val onSwipedOut: ((SwipeDirection) -> Unit)?,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@SwipeableModifier
}
