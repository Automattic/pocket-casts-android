package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp")
class RearrangeDragStateTest {
    @get:Rule
    val rule = createComposeRule()

    private val items = mutableStateListOf<Int>().apply { addAll(0 until 30) }
    private val moves = mutableListOf<Pair<Int, Int>>()
    private val starts = mutableListOf<Any>()
    private val drops = mutableListOf<Any>()
    private var isShown by mutableStateOf(true)

    @Test
    fun `auto scrolling past a row moves it without reverting`() {
        setContent()
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag("handle8").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, 6.dp.toPx())) }
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("handle8").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)

        assertTrue(moves.isNotEmpty())
        assertTrue(moves.toString(), moves.zipWithNext().none { (first, second) -> second == first.second to first.first })
    }

    @Test
    fun `a new drag cannot start while the previous drop is recovering`() {
        setContent()
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag("handle1").performTouchInput {
            down(center)
            up()
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("handle2").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, 6.dp.toPx())) }
            up()
        }
        rule.mainClock.advanceTimeBy(2_000)

        assertEquals(listOf<Any>(1), starts)
        assertEquals(listOf<Any>(1), drops)
    }

    @Test
    fun `closing the screen while the drop is recovering still drops`() {
        setContent()
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag("handle1").performTouchInput {
            down(center)
            up()
        }
        rule.mainClock.advanceTimeBy(100)
        isShown = false
        rule.mainClock.advanceTimeBy(100)

        assertEquals(listOf<Any>(1), drops)
    }

    @Test
    fun `a second finger on a handle does not start the next gesture`() {
        setContent()
        val row0 = rule.onNodeWithTag("row0").fetchSemanticsNode().boundsInRoot
        val handle3 = rule.onNodeWithTag("handle3").fetchSemanticsNode().boundsInRoot

        rule.onRoot().performTouchInput {
            down(0, Offset(10f, row0.center.y))
            down(1, handle3.center)
            up(1)
            up(0)
        }
        rule.waitForIdle()
        rule.onNodeWithTag("row5").performTouchInput {
            down(Offset(10f, centerY))
            up()
        }
        rule.waitForIdle()

        assertEquals(emptyList<Any>(), starts)
    }

    private fun setContent() {
        rule.setContent {
            if (!isShown) return@setContent
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val haptics = LocalHapticFeedback.current
            val state = remember { RearrangeDragState(listState, scope, haptics, liftElevation = 0f, maxDragScroll = 20) }
            state.isReorderable = { true }
            state.onMove = { from, to ->
                val move = items.indexOf(from) to items.indexOf(to)
                moves += move
                items.add(move.second, items.removeAt(move.first))
            }
            state.onStartDrag = { starts += it }
            state.onDrop = { drops += it }
            val rows = items.toList()
            LazyColumn(state = listState, modifier = Modifier.height(500.dp).rearrangeDragGestures(state)) {
                items(rows, key = { it }) { item ->
                    Box(
                        modifier = Modifier
                            .testTag("row$item")
                            .fillMaxWidth()
                            .height(50.dp)
                            .graphicsLayer { translationY = state.translationY(item) },
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("handle$item")
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .pointerInput(item) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        state.onHandleDown(item)
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}
