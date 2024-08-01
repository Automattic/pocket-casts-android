package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class CustomMenuItemOption(val id: Int) {
    Share(1),
    SelectAll(2),
    ;

    val titleResource: Int
        get() = when (this) {
            Share -> LR.string.share
            SelectAll -> LR.string.select_all
        }
    val order = id
}
