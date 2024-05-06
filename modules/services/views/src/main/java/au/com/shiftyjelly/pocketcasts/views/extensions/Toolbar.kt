package au.com.shiftyjelly.pocketcasts.views.extensions

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.None
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors

fun Toolbar.updateColors(toolbarColors: ToolbarColors, navigationIcon: NavigationIcon) {
    setBackgroundColor(toolbarColors.backgroundColor)
    setTitleTextColor(toolbarColors.titleColor)
    setNavigationIcon(if (navigationIcon.icon == null) null else context.getTintedDrawable(drawableId = navigationIcon.icon, tintColor = toolbarColors.iconColor))
    menu.tintIcons(toolbarColors.iconColor, toolbarColors.excludeMenuItems)
    overflowIcon?.setTintList(ColorStateList.valueOf(toolbarColors.iconColor))
}

fun Toolbar.setup(
    title: String? = null,
    @MenuRes menu: Int? = null,
    chromeCastButton: ChromeCastButton = ChromeCastButton.None,
    navigationIcon: NavigationIcon = None,
    onNavigationClick: (() -> Unit)? = null,
    activity: Activity?,
    theme: Theme,
    toolbarColors: ToolbarColors? = ToolbarColors.Theme(theme = theme, context = context),
) {
    if (title != null) {
        setTitle(title)
    }
    if (menu != null) {
        this.menu.clear()
        inflateMenu(menu)
    }
    when (chromeCastButton) {
        ChromeCastButton.None -> {}
        is ChromeCastButton.Shown ->
            setupChromeCastButton(context) {
                chromeCastButton.chromeCastAnalytics.trackChromeCastViewShown()
            }
    }
    if (toolbarColors != null) {
        updateColors(toolbarColors = toolbarColors, navigationIcon = navigationIcon)
    }
    if (navigationIcon.contentDescription != null) {
        this.navigationContentDescription = context.getString(navigationIcon.contentDescription)
    }
    if (navigationIcon != None) {
        setNavigationOnClickListener {
            if (onNavigationClick == null) {
                @Suppress("DEPRECATION")
                activity?.onBackPressed()
            } else {
                onNavigationClick()
            }
        }
    }
    if (activity != null) {
        setOnLongClickListener {
            theme.toggleDarkLightThemeActivity(activity as AppCompatActivity)
            true
        }
    }
}

fun Toolbar.setupChromeCastButton(context: Context?, onClick: () -> Unit) {
    menu.setupChromeCastButton(context, onClick)
}
