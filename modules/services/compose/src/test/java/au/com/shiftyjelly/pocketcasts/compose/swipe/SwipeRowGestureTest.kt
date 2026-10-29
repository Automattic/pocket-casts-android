package au.com.shiftyjelly.pocketcasts.compose.swipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sign
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Config(qualifiers = "w411dp-h891dp-mdpi")
class SwipeRowGestureMdpiTest : SwipeRowGestureTest()

@Config(qualifiers = "w411dp-h891dp-xhdpi")
class SwipeRowGestureXhdpiTest : SwipeRowGestureTest()

@Config(qualifiers = "w411dp-h891dp-xxxhdpi")
class SwipeRowGestureXxxhdpiTest : SwipeRowGestureTest()

@RunWith(RobolectricTestRunner::class)
abstract class SwipeRowGestureTest {
    @get:Rule
    val rule = createComposeRule()

    private val clicks = mutableMapOf<String, Int>()
    private var isSwipeEnabled by mutableStateOf(true)
    private val haptics = mutableListOf<HapticFeedbackType>()

    @Before
    fun setup() {
        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides RecordingHapticFeedback(haptics)) {
                SwipeRow(
                    state = rememberSwipeRowState(),
                    leadingActions = listOf(action("Top"), action("Bottom"), action("Share")),
                    trailingActions = listOf(action("Delete")),
                    isSwipeEnabled = isSwipeEnabled,
                    modifier = Modifier.testTag(ROW),
                ) {
                    Box(
                        modifier = Modifier
                            .testTag(CONTENT)
                            .fillMaxWidth()
                            .height(72.dp)
                            .clickable { clicks.increment(CONTENT) },
                    )
                }
            }
        }
        advance()
    }

    @Test
    fun `release past a third of the buttons opens the row`() {
        drag((-30).dp)

        assertContentAt((-72).dp)
    }

    @Test
    fun `release before a third of the buttons closes the row`() {
        drag((-20).dp)

        assertContentAt(0.dp)
    }

    @Test
    fun `swiping back keeps the row open until two thirds of the buttons`() {
        drag((-30).dp)
        drag(20.dp)

        assertContentAt((-72).dp)
    }

    @Test
    fun `swiping back past two thirds of the buttons closes the row`() {
        drag((-30).dp)
        drag(30.dp)

        assertContentAt(0.dp)
    }

    @Test
    fun `three leading buttons open to their combined width`() {
        drag(100.dp)

        assertContentAt(216.dp)
    }

    @Test
    fun `releasing a drag past the full swipe threshold fires the first action once`() {
        drag((-210).dp)

        assertEquals(mapOf("Delete" to 1), clicks)
        assertContentAt(0.dp)
    }

    @Test
    fun `releasing a drag below the full swipe threshold opens the row`() {
        drag((-190).dp)

        assertEquals(emptyMap<String, Int>(), clicks)
        assertContentAt((-72).dp)
    }

    @Test
    fun `full swipe threshold grows with the number of buttons`() {
        drag(230.dp)

        assertEquals(emptyMap<String, Int>(), clicks)
        assertContentAt(216.dp)
    }

    @Test
    fun `crossing the full swipe threshold performs haptic feedback`() {
        rule.onNodeWithTag(ROW).performTouchInput {
            down(center)
            moveBy(Offset(-viewConfiguration.touchSlop - 100.dp.toPx(), 0f))
        }
        advance()
        assertEquals(emptyList<HapticFeedbackType>(), haptics)

        rule.onNodeWithTag(ROW).performTouchInput { moveBy(Offset(-150.dp.toPx(), 0f)) }
        advance()
        assertEquals(listOf(HapticFeedbackType.ContextClick), haptics)
    }

    @Test
    fun `flinging past the full swipe threshold fires the first action once`() {
        fling((-220).dp)

        assertEquals(mapOf("Delete" to 1), clicks)
        assertContentAt(0.dp)
    }

    @Test
    fun `flinging below the full swipe threshold opens the row`() {
        fling((-120).dp)

        assertEquals(emptyMap<String, Int>(), clicks)
        assertContentAt((-72).dp)
    }

    @Test
    fun `disabled swipe ignores drags`() {
        isSwipeEnabled = false
        advance()

        drag((-210).dp)

        assertEquals(emptyMap<String, Int>(), clicks)
        assertContentAt(0.dp)
    }

    @Test
    fun `disabling swipe closes an open row`() {
        drag((-30).dp)

        isSwipeEnabled = false
        advance()

        assertContentAt(0.dp)
    }

    @Test
    fun `tapping a revealed button fires its action and closes the row`() {
        drag((-30).dp)

        rule.onNodeWithContentDescription("Delete").performClick()
        advance()

        assertEquals(mapOf("Delete" to 1), clicks)
        assertContentAt(0.dp)
    }

    @Test
    fun `tapping an open row closes it without clicking it`() {
        drag((-30).dp)

        rule.onNodeWithTag(CONTENT).performClick()
        advance()

        assertEquals(emptyMap<String, Int>(), clicks)
        assertContentAt(0.dp)
    }

    private fun action(name: String) = SwipeRowAction(
        iconId = IR.drawable.ic_share,
        contentDescription = name,
        backgroundColor = Color.Red,
        isFullSwipeEnabled = true,
        onClick = { state ->
            state.settle()
            clicks.increment(name)
        },
    )

    private fun drag(distance: Dp) {
        rule.onNodeWithTag(ROW).performTouchInput {
            val total = distance.toPx() + distance.value.sign * viewConfiguration.touchSlop
            down(center)
            repeat(10) { moveBy(Offset(total / 10, 0f)) }
            advanceEventTime(200)
            up()
        }
        advance()
    }

    private fun fling(distance: Dp) {
        rule.onNodeWithTag(ROW).performTouchInput {
            swipe(center, center + Offset(distance.toPx() + distance.value.sign * viewConfiguration.touchSlop, 0f), durationMillis = 48)
        }
        advance()
    }

    private fun advance() {
        rule.waitForIdle()
    }

    private fun assertContentAt(left: Dp) {
        rule.onNodeWithTag(CONTENT).assertLeftPositionInRootIsEqualTo(left)
    }

    private fun MutableMap<String, Int>.increment(key: String) {
        this[key] = getOrDefault(key, 0) + 1
    }

    private companion object {
        const val ROW = "row"
        const val CONTENT = "content"
    }
}

private class RecordingHapticFeedback(
    private val haptics: MutableList<HapticFeedbackType>,
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        haptics += hapticFeedbackType
    }
}
