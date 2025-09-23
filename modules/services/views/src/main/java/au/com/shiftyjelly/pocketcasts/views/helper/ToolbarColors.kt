package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastData

@Parcelize
data class ToolbarColors(
    @ColorInt val iconColor: Int,
    @ColorInt val titleColor: Int,
    @ColorInt val backgroundColor: Int,
    val excludeMenuItems: List<Int>? = null,
) : Parcelable {
    val iconComposeColor get() = Color(iconColor)
    val titleComposeColor get() = Color(titleColor)
    val backgroundComposeColor get() = Color(backgroundColor)

    companion object {
        fun theme(theme: Theme, context: Context, excludeMenuItems: List<Int>? = null) = ToolbarColors(
            iconColor = theme.getToolbarIconColor(context),
            titleColor = theme.getToolbarTextColor(context),
            backgroundColor = theme.getToolbarBackgroundColor(context),
            excludeMenuItems = excludeMenuItems,
        )

        fun user(@ColorInt color: Int, theme: Theme) = ToolbarColors(
            iconColor = ThemeColor.filterIcon01(theme.activeTheme, color),
            titleColor = ThemeColor.filterText01(theme.activeTheme, color),
            backgroundColor = ThemeColor.filterUi01(theme.activeTheme, color),
        )

        fun podcast(podcast: PodcastData, theme: Theme) = ToolbarColors(
            iconColor = ThemeColor.podcastIcon01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
            titleColor = ThemeColor.podcastText01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
            backgroundColor = ThemeColor.podcastUi01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
        )

        fun podcast(lightColor: Int, darkColor: Int, theme: Theme) = ToolbarColors(
            iconColor = ThemeColor.podcastIcon01(theme.activeTheme, if (theme.isDarkTheme) darkColor else lightColor),
            titleColor = ThemeColor.podcastText01(theme.activeTheme, if (theme.isDarkTheme) darkColor else lightColor),
            backgroundColor = ThemeColor.podcastUi01(theme.activeTheme, if (theme.isDarkTheme) darkColor else lightColor),
        )

        fun podcast(lightColor: Int, darkColor: Int, theme: Theme.ThemeType) = ToolbarColors(
            iconColor = ThemeColor.podcastIcon01(theme, if (theme.darkTheme) darkColor else lightColor),
            titleColor = ThemeColor.podcastText01(theme, if (theme.darkTheme) darkColor else lightColor),
            backgroundColor = ThemeColor.podcastUi01(theme, if (theme.darkTheme) darkColor else lightColor),
        )
    }
}
