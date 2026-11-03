package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimatedPlayButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `describes the action for the current state`() {
        var isPlaying by mutableStateOf(false)
        composeTestRule.setContent {
            AnimatedPlayButton(
                isPlaying = isPlaying,
                onClick = {},
                iconSize = 24.dp,
                iconTint = Color.White,
                modifier = Modifier.size(64.dp),
            )
        }

        composeTestRule.onNodeWithContentDescription("Play button").assertIsDisplayed()

        isPlaying = true

        composeTestRule.onNodeWithContentDescription("Pause button").assertIsDisplayed()
    }

    @Test
    fun `clicking the button invokes the callback once`() {
        var clicks = 0
        composeTestRule.setContent {
            AnimatedPlayButton(
                isPlaying = false,
                onClick = { clicks++ },
                iconSize = 24.dp,
                iconTint = Color.White,
                modifier = Modifier.size(64.dp),
            )
        }

        composeTestRule.onNodeWithContentDescription("Play button").performClick()

        assertEquals(1, clicks)
    }
}
