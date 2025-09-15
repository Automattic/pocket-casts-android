package au.com.shiftyjelly.pocketcasts.views.swipe

import android.content.Context
import android.graphics.Color
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

enum class SwipeAction : SwipeButton.UiState {
    AddToUpNextTop,
    AddToUpNextBottom,
    RemoveFromUpNext,
    Share,
    Archive,
    Unarchive,
    Remove,
    ;

    override fun contentDescription(context: Context) = when (this) {
        AddToUpNextTop -> context.getString(LR.string.add_to_up_next_top)
        AddToUpNextBottom -> context.getString(LR.string.add_to_up_next_bottom)
        RemoveFromUpNext -> context.getString(LR.string.remove_from_up_next)
        Share -> context.getString(LR.string.share)
        Archive -> context.getString(LR.string.archive)
        Unarchive -> context.getString(LR.string.unarchive)
        Remove -> context.getString(LR.string.remove_from_playlist)
    }

    override fun backgroundTint(context: Context): Int {
        val id = when (this) {
            AddToUpNextTop -> UR.attr.support_04
            AddToUpNextBottom -> UR.attr.support_03
            RemoveFromUpNext -> UR.attr.support_05
            Share -> UR.attr.support_01
            Archive -> UR.attr.support_06
            Unarchive -> UR.attr.support_06
            Remove -> UR.attr.support_05
        }
        return context.getThemeColor(id)
    }

    override fun imageTint(context: Context) = Color.WHITE

    override fun imageDrawableId() = when (this) {
        AddToUpNextTop -> IR.drawable.ic_upnext_movetotop
        AddToUpNextBottom -> IR.drawable.ic_upnext_movetobottom
        RemoveFromUpNext -> IR.drawable.ic_upnext_remove
        Share -> IR.drawable.ic_share
        Archive -> IR.drawable.ic_archive
        Unarchive -> IR.drawable.ic_unarchive
        Remove -> IR.drawable.ic_delete
    }
}
