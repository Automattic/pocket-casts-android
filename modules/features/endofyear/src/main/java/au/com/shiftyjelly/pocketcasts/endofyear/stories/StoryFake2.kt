package au.com.shiftyjelly.pocketcasts.endofyear.stories

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.models.entity.Episode

class StoryFake2(
    val episode: Episode,
) : Story() {
    override val backgroundColor: Color = Color.Green
}
