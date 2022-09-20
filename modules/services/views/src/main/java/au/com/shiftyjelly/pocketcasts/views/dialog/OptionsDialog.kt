package au.com.shiftyjelly.pocketcasts.views.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnLayout
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.ToggleButtonOption
import au.com.shiftyjelly.pocketcasts.compose.dialogs.OptionsDialogComponent
import au.com.shiftyjelly.pocketcasts.compose.dialogs.OptionsDialogOption
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class OptionsDialog : BottomSheetDialogFragment() {

    @Inject lateinit var theme: Theme

    private var title: String? = null
    private var iconColor: Int? = null
    private val options = mutableListOf<OptionsDialogOption>()
    private var onDismiss: (() -> Unit)? = null
    private var forceDarkTheme: Boolean = false

    fun setForceDarkTheme(force: Boolean): OptionsDialog {
        this.forceDarkTheme = force
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (!forceDarkTheme) {
            return super.onCreateDialog(savedInstanceState)
        }

        val context = ContextThemeWrapper(requireContext(), UR.style.ThemeDark)
        return BottomSheetDialog(context, UR.style.BottomSheetDialogThemeDark)
    }

    fun setTitle(title: String): OptionsDialog {
        this.title = title
        return this
    }

    fun setIconColor(@ColorInt color: Int): OptionsDialog {
        this.iconColor = color
        return this
    }

    fun addTextOption(@StringRes titleId: Int? = null, titleString: String? = null, @ColorRes titleColor: Int? = null, @StringRes valueId: Int? = null, @DrawableRes imageId: Int? = null, @ColorRes imageColor: Int? = null, click: (() -> Unit)): OptionsDialog {
        addOption(titleId = titleId, titleString = titleString, titleColor = titleColor, valueId = valueId, imageId = imageId, imageColor = imageColor, click = click)
        return this
    }

    fun addCheckedOption(@StringRes titleId: Int? = null, titleString: String? = null, @DrawableRes imageId: Int? = null, checked: Boolean = false, click: (() -> Unit)): OptionsDialog {
        addOption(titleId = titleId, titleString = titleString, imageId = imageId, checked = checked, click = click)
        return this
    }

    fun addToggleOptions(@StringRes titleId: Int, @DrawableRes imageId: Int? = null, vararg options: ToggleButtonOption): OptionsDialog {
        addOption(titleId = titleId, imageId = imageId, toggleOptions = listOf(*options))
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss?.invoke()
    }

    private fun addOption(@StringRes titleId: Int?, titleString: String? = null, @ColorRes titleColor: Int? = null, @StringRes valueId: Int? = null, @DrawableRes imageId: Int? = null, @ColorRes imageColor: Int? = null, toggleOptions: List<ToggleButtonOption>? = null, checked: Boolean = false, click: (() -> Unit)? = null, onSwitch: ((switchedOn: Boolean) -> Unit)? = null) {
        var closeAndClick: (() -> Unit)? = null
        if (click != null) {
            closeAndClick = {
                click.invoke()
                dismiss()
            }
        }

        options.add(OptionsDialogOption(titleId = titleId, titleString = titleString, titleColor = titleColor, valueId = valueId, imageId = imageId, imageColor = imageColor, checked = checked, click = closeAndClick, toggleOptions = toggleOptions, onSwitch = onSwitch))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = if (forceDarkTheme) ContextThemeWrapper(context, UR.style.ThemeDark) else context ?: throw Exception("Context not found")
        return ComposeView(context).apply {
            setContent {
                AppTheme(if (forceDarkTheme) Theme.ThemeType.DARK else theme.activeTheme) {
                    OptionsDialogComponent(title = title, iconColor = iconColor, options = options)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).run {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    peekHeight = 0
                    skipCollapsed = true
                }
            }
        }
    }
}
