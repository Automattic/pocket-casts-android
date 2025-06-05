package au.com.shiftyjelly.pocketcasts.compose

import android.os.Parcelable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<Color, ColorParceler>
data class PodcastColors(
    val background: Color,
    val playerTint: Color,
) : Parcelable {
    constructor(podcast: Podcast) : this(
        background = Color(podcast.backgroundColor),
        playerTint = Color(podcast.getPlayerTintColor(isDarkTheme = true)),
    )

    companion object {
        val ForUserEpisode = PodcastColors(
            background = Color(0xFF3D3D3D),
            playerTint = Color.White,
        )

        val TheDailyPreview
            get() = PodcastColors(
                background = Color(0xFF0477C2),
                playerTint = Color(0xFFCFEB7B),
            )

        val ThisAmericanLifePreview
            get() = PodcastColors(
                background = Color(0xFFEC0404),
                playerTint = Color(0xFFF47C84),
            )

        val ConanPreview
            get() = PodcastColors(
                background = Color(0xFF37444F),
                playerTint = Color(0xFFF87509),
            )

        val DarknetDiariesPreview
            get() = PodcastColors(
                background = Color(0xFF2E2D2D),
                playerTint = Color(0xFFFF3232),
            )
    }
}

val LocalPodcastColors = staticCompositionLocalOf<PodcastColors?> { null }

class PodcastColorsParameterProvider : PreviewParameterProvider<PodcastColors> {
    override val values = sequenceOf(
        PodcastColors.TheDailyPreview,
        PodcastColors.ThisAmericanLifePreview,
        PodcastColors.ConanPreview,
        PodcastColors.DarknetDiariesPreview,
    )
}
