package au.com.shiftyjelly.pocketcasts.compose.dialogs

import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption

data class OptionsDialogOption(
    val titleId: Int?,
    val titleString: String? = null,
    val titleColor: Int? = null,
    val valueId: Int? = null,
    val imageId: Int? = null,
    val imageColor: Int? = null,
    val toggleOptions: List<ToggleButtonOption>? = null,
    val checked: Boolean = false,
    val click: (() -> Unit)?,
    val onSwitch: ((switchedOn: Boolean) -> Unit)? = null,
)
