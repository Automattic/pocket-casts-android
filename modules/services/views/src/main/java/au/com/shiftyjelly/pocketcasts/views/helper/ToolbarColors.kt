package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor

sealed class ToolbarColors(
    @ColorInt val iconColor: Int,
    @ColorInt val titleColor: Int,
    @ColorInt val backgroundColor: Int,
    val excludeMenuItems: List<Int>?
) {
    class Theme(theme: au.com.shiftyjelly.pocketcasts.ui.theme.Theme, context: Context, excludeMenuItems: List<Int>? = null) : ToolbarColors(
        iconColor = theme.getToolbarIconColor(context),
        titleColor = theme.getToolbarTextColor(context),
        backgroundColor = theme.getToolbarBackgroundColor(context),
        excludeMenuItems = excludeMenuItems
    )

    class User(@ColorInt color: Int, theme: au.com.shiftyjelly.pocketcasts.ui.theme.Theme, excludeMenuItems: List<Int>? = null) : ToolbarColors(
        iconColor = ThemeColor.filterIcon01(theme.activeTheme, color),
        titleColor = ThemeColor.filterText01(theme.activeTheme, color),
        backgroundColor = ThemeColor.filterUi01(theme.activeTheme, color),
        excludeMenuItems = excludeMenuItems
    )

    class Podcast(podcast: au.com.shiftyjelly.pocketcasts.models.entity.Podcast, theme: au.com.shiftyjelly.pocketcasts.ui.theme.Theme, excludeMenuItems: List<Int>? = null) : ToolbarColors(
        iconColor = ThemeColor.podcastIcon01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
        titleColor = ThemeColor.podcastText01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
        backgroundColor = ThemeColor.podcastUi01(theme.activeTheme, podcast.getTintColor(theme.isDarkTheme)),
        excludeMenuItems = excludeMenuItems
    )
}
