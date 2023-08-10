package au.com.shiftyjelly.pocketcasts.compose.dialogs

import android.os.Parcelable
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption
import kotlinx.parcelize.Parcelize

@Parcelize
data class OptionsDialogOption(
    val titleId: Int?,
    val titleString: String?,
    val titleColor: Int?,
    val valueId: Int?,
    val imageId: Int?,
    val imageColor: Int?,
    val toggleOptions: List<ToggleButtonOption>? = null,
    val checked: Boolean = false,
    val click: (() -> Unit)?,
    val onSwitch: ((switchedOn: Boolean) -> Unit)? = null
) : Parcelable
