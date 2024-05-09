package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast as PodcastData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme as UiTheme

@Parcelize
data class ToolbarColors(
    @ColorInt val iconColor: Int,
    @ColorInt val titleColor: Int,
    @ColorInt val backgroundColor: Int,
    val excludeMenuItems: List<Int>?,
) : Parcelable {
    companion object {
        fun theme(theme: UiTheme, context: Context, excludeMenuItems: List<Int>? = null) = ToolbarColors(
            iconColor = theme.getToolbarIconColor(context),
            titleColor = theme.getToolbarTextColor(context),
            backgroundColor = theme.getToolbarBackgroundColor(context),
            excludeMenuItems = excludeMenuItems,
        )

        fun user(@ColorInt color: Int, theme: UiTheme, excludeMenuItems: List<Int>? = null) = ToolbarColors(
            iconColor = ThemeColor.filterIcon01(theme.activeTheme, color),
            titleColor = ThemeColor.filterText01(theme.activeTheme, color),
            backgroundColor = ThemeColor.filterUi01(theme.activeTheme, color),
            excludeMenuItems = excludeMenuItems,
        )

        fun podcast(podcast: PodcastData, theme: UiTheme, excludeMenuItems: List<Int>? = null) = ToolbarColors(
            iconColor = ThemeColor.podcastIcon01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
            titleColor = ThemeColor.podcastText01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
            backgroundColor = ThemeColor.podcastUi01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
            excludeMenuItems = excludeMenuItems,
        )

        fun podcast(lightColor: Int, darkColor: Int, theme: Theme, excludeMenuItems: List<Int>? = null) = ToolbarColors(
            iconColor = ThemeColor.podcastIcon01(theme.activeTheme, if (theme.isDarkTheme) darkColor else lightColor),
            titleColor = ThemeColor.podcastText01(theme.activeTheme, if (theme.isDarkTheme) darkColor else lightColor),
            backgroundColor = ThemeColor.podcastUi01(theme.activeTheme, if (theme.isDarkTheme) darkColor else lightColor),
            excludeMenuItems = excludeMenuItems,
        )
    }
}
