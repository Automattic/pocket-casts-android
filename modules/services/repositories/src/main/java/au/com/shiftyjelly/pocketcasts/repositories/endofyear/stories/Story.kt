package au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.utils.seconds

abstract class Story {
    abstract val identifier: String
    open val storyLength: Long = 5.seconds()
    open val backgroundColor: Color = Color.Black
    open val plusOnly: Boolean = false
    val tintColor: Color = TintColor
    val subtitleColor: Color = SubtitleColor
    open val shareable: Boolean = true

    companion object {
        val TintColor = Color(0xFFFBFBFC)
        val SubtitleColor = Color(0xFF8F97A4)
    }
}
