package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val name: String,
    val podcasts: List<String>,
    val color: Int,
) : Parcelable
