package au.com.shiftyjelly.pocketcasts.endofyear.stories

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.utils.seconds

abstract class Story {
    open val storyLength: Long = 2.seconds()
    open val backgroundColor: Color = Color.Transparent
    val tintColor: Color = Color.White
    val isInteractive: Boolean = false
}
