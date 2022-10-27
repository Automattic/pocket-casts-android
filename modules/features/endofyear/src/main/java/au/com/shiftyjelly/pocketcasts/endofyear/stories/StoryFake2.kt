package au.com.shiftyjelly.pocketcasts.endofyear.stories

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.utils.seconds

class StoryFake2(
    val episode: Episode,
) : Story() {
    override val storyLength: Long = 3.seconds()
    override val backgroundColor: Color = Color.Green
}
