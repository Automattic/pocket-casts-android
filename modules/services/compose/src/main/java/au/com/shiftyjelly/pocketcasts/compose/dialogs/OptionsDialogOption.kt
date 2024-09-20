package au.com.shiftyjelly.pocketcasts.compose.dialogs

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption

data class OptionsDialogOption constructor(
    val titleId: Int?,
    val titleString: String? = null,
    val titleColor: Int? = null,
    val valueId: Int? = null,
    val imageId: Int? = null,
    val imageColor: Int? = null,
    val backgroundColor: Color? = null,
    val toggleOptions: List<ToggleButtonOption>? = null,
    val checked: Boolean = false,
    val click: (() -> Unit)?,
    val onSwitch: ((switchedOn: Boolean) -> Unit)? = null,
)
