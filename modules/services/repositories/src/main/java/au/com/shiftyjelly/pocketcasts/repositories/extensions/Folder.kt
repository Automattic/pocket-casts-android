package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.entity.Folder

private val AUTOMOTIVE_DRAWABLES = arrayOf(
    R.drawable.auto_folder_01,
    R.drawable.auto_folder_02,
    R.drawable.auto_folder_03,
    R.drawable.auto_folder_04,
    R.drawable.auto_folder_05,
    R.drawable.auto_folder_06,
    R.drawable.auto_folder_07
)

val Folder.automotiveDrawableId: Int
    get() = AUTOMOTIVE_DRAWABLES.getOrNull(color) ?: AUTOMOTIVE_DRAWABLES.first()
