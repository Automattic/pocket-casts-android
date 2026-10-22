package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TvCategoryStyle {
    private val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFF53869), Color(0xFFFA5245))),
        Brush.linearGradient(listOf(Color(0xFF6145E8), Color(0xFFE84A8A))),
        Brush.linearGradient(listOf(Color(0xFF03A8F5), Color(0xFF4FD1F2))),
    )

    fun gradient(index: Int): Brush {
        val size = gradients.size
        return gradients[((index % size) + size) % size]
    }
}
