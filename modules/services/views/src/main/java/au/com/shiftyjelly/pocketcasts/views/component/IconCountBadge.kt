package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import com.google.android.material.badge.BadgeDrawable

fun createIconCountBadge(context: Context) =
    BadgeDrawable.create(context).apply {
        badgeGravity = BadgeDrawable.TOP_END
        badgeTextColor = context.getThemeColor(R.attr.primary_ui_01)
        backgroundColor = context.getThemeColor(R.attr.primary_icon_01)
        verticalOffset = 14.dpToPx(context.resources.displayMetrics)
        horizontalOffset = 12.dpToPx(context.resources.displayMetrics)
    }