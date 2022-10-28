package au.com.shiftyjelly.pocketcasts.endofyear.stories

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

class StoryFake1(
    val podcasts: List<Podcast>,
) : Story() {
    override val backgroundColor: Color = Color.Magenta
}
