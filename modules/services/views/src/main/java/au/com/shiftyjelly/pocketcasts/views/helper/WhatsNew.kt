package au.com.shiftyjelly.pocketcasts.views.helper

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val blogLink = "https://blog.pocketcasts.com/7-20-changes/"

object WhatsNew {
    val pages = listOf(
        WhatsNewPage(
            items = listOf(
                WhatsNewItem.Image(IR.drawable.whatsnew_folder, IR.drawable.plus_logo),
                WhatsNewItem.Title(LR.string.whats_new_folders_title_7_20),
                WhatsNewItem.Body(LR.string.whats_new_folders_7_20),
                WhatsNewItem.Link(LR.string.whats_new_blog_link, blogLink)
            )
        ),
        WhatsNewPage(
            items = listOf(
                WhatsNewItem.Image(IR.drawable.whatsnew_grid_sync),
                WhatsNewItem.Title(LR.string.whats_new_grid_title_7_20),
                WhatsNewItem.Body(LR.string.whats_new_grid_7_20),
                WhatsNewItem.Link(LR.string.whats_new_blog_link, blogLink)
            )
        )
    )

    fun isWhatsNewNewerThan(versionCode: Int?): Boolean {
        return Settings.WHATS_NEW_VERSION_CODE > (versionCode ?: 0)
    }
}

data class WhatsNewPage(val items: List<WhatsNewItem>)

sealed class WhatsNewItem {
    data class Image(@DrawableRes val resourceName: Int, @DrawableRes val secondaryResourceName: Int? = null) : WhatsNewItem()
    data class Title(@StringRes val title: Int) : WhatsNewItem()
    data class Body(@StringRes val body: Int) : WhatsNewItem()
    data class Bullet(@StringRes val body: Int) : WhatsNewItem()
    data class Link(@StringRes val title: Int, val url: String) : WhatsNewItem()
}
