package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class CustomMenuItemOption(val id: Int) {
    Share(1),
    Bookmark(2),
    ;

    val titleResource: Int
        get() = when (this) {
            Share -> LR.string.share
            Bookmark -> LR.string.bookmark
        }
    val order = id
}
