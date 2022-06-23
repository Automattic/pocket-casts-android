/*
 * Copyright 2021 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.shiftyjelly.pocketcasts.compose.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor

/**
 * This is a workaround for https://issuetracker.google.com/issues/174348612, which means that nested scrolling layouts
 * in Compose do not work as nested scrolling children in the view system.
 *
 * Some common scenarios:
 *
 * - Using a `ComposeView` + `LazyColumn()` in a `CoordinatorLayout` with `AppBarLayout`.
 * - Using a `ComposeView` + `LazyColumn()` in a `BottomSheetDialog`.
 *
 * This workaround [NestedScrollConnection] is designed to be used at the top of your Compose layout. It will then
 * dispatch the necessary nested scrolling calls up the view tree.
 *
 * ```
 * setContent {
 *     Surface(
 *         // Add this somewhere near the top of your layout, above any scrolling layouts
 *         modifier = Modifier.nestedScroll(rememberViewInteropNestedScrollConnection())
 *     ) {
 *         LazyColumn() {
 *             // blah
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun rememberViewInteropNestedScrollConnection(
    view: View = LocalView.current
): NestedScrollConnection = remember(view) {
    ViewInteropNestedScrollConnection(view)
}

/**
 * Workaround for https://issuetracker.google.com/issues/174348612
 */
internal class ViewInteropNestedScrollConnection(
    private val view: View,
) : NestedScrollConnection {
    private val tmpArray by lazy(LazyThreadSafetyMode.NONE) { IntArray(2) }

    private val viewHelper by lazy(LazyThreadSafetyMode.NONE) {
        NestedScrollingChildHelper(view).apply {
            // Everything in Compose has nested scroll enabled
            isNestedScrollingEnabled = true
        }
    }

    init {
        // We need to enable nested scrolling on the ComposeView too
        ViewCompat.setNestedScrollingEnabled(view, true)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // startNestedScroll() will automatically no-op if a nested scroll is already in progress
        if (viewHelper.startNestedScroll(available.guessScrollAxis(), source.toViewType())) {
            val parentConsumed = tmpArray.apply { fill(0) }

            // Compose uses opposite signs to the view system. Positive Y = up in Compose, down in views, etc.
            // We need to use a relative ceil for rounding too, to ensure that views can fully consume what's available.
            viewHelper.dispatchNestedPreScroll(
                available.x.ceilAwayFromZero().toInt() * -1, // dx
                available.y.ceilAwayFromZero().toInt() * -1, // dy
                parentConsumed, // consumed
                null, // offsetInWindow
                source.toViewType() // type
            )

            // toOffset coerces the parentConsumed values within `available`
            return toOffset(parentConsumed, available)
        }

        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        // startNestedScroll() will automatically no-op if a nested scroll is already in progress
        if (viewHelper.startNestedScroll(available.guessScrollAxis(), source.toViewType())) {
            val parentConsumed = tmpArray.apply { fill(0) }

            // Compose uses opposite signs to the view system. Positive Y = up in Compose, down in views, etc.
            // We need to use a relative ceil for rounding too, to ensure that views can fully consume what's available.
            viewHelper.dispatchNestedScroll(
                consumed.x.ceilAwayFromZero().toInt() * -1, // dxConsumed
                consumed.y.ceilAwayFromZero().toInt() * -1, // dyConsumed
                available.x.ceilAwayFromZero().toInt() * -1, // dxUnconsumed
                available.y.ceilAwayFromZero().toInt() * -1, // dyUnconsumed
                null, // offsetInWindow
                source.toViewType(), // type
                parentConsumed, // consumed
            )

            // toOffset coerces the parentConsumed values within `available`
            return toOffset(parentConsumed, available)
        }

        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val dispatched = viewHelper.dispatchNestedPreFling(
            available.x * -1f, // velocityX
            available.y * -1f, // velocityY
        ) || viewHelper.dispatchNestedFling(
            available.x * -1f, // velocityX
            available.y * -1f, // velocityY
            // consumed. We don't know at this point if the fling was consumed by the child or not, so we assume yes.
            // In reality most parents don't look at this value.
            true,
        )

        // Stop any nested scrolls which are on-going...
        if (viewHelper.hasNestedScrollingParent(TYPE_TOUCH)) {
            viewHelper.stopNestedScroll(TYPE_TOUCH)
        } else if (viewHelper.hasNestedScrollingParent(TYPE_NON_TOUCH)) {
            viewHelper.stopNestedScroll(TYPE_NON_TOUCH)
        }

        return if (dispatched) available else Velocity.Zero
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Float.ceilAwayFromZero(): Float = if (this >= 0) ceil(this) else floor(this)

/**
 * This transforms the view nested scrolling consumed array into an [Offset], coercing the values by
 * the [originalOffset] (needed due to Int <> Float rounding).
 */
private fun toOffset(consumed: IntArray, originalOffset: Offset): Offset {
    require(consumed.size == 2)

    val x = (consumed[0] * -1f).let {
        when {
            originalOffset.x >= 0 -> it.coerceAtMost(originalOffset.x)
            else -> it.coerceAtLeast(originalOffset.x)
        }
    }
    val y = (consumed[1] * -1f).let {
        when {
            originalOffset.y >= 0 -> it.coerceAtMost(originalOffset.y)
            else -> it.coerceAtLeast(originalOffset.y)
        }
    }

    return Offset(x, y)
}

private fun NestedScrollSource.toViewType(): Int = when (this) {
    NestedScrollSource.Drag -> TYPE_TOUCH
    else -> TYPE_NON_TOUCH
}

private fun Offset.guessScrollAxis(): Int {
    var axes = ViewCompat.SCROLL_AXIS_NONE
    if (x.absoluteValue >= 0.5f) {
        axes = axes or ViewCompat.SCROLL_AXIS_HORIZONTAL
    }
    if (y.absoluteValue >= 0.5f) {
        axes = axes or ViewCompat.SCROLL_AXIS_VERTICAL
    }
    return axes
}
