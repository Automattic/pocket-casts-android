package au.com.shiftyjelly.pocketcasts.views.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.view.doOnLayout
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
open class ConfirmationDialog : BottomSheetDialogFragment() {
    sealed class ButtonType(val text: String) {
        data class Normal(val textString: String) : ButtonType(textString)
        data class Danger(val textString: String) : ButtonType(textString)
    }

    private var title: String? = null
    private var summary: String? = null

    @DrawableRes private var iconId: Int = 0
    private lateinit var buttonType: ButtonType
    var secondaryType: ButtonType? = null

    @AttrRes private var iconTintAttr: Int? = UR.attr.primary_icon_01

    @AttrRes private var secondaryTextColorAttr: Int? = null

    @AttrRes private var summaryTextColorAttr: Int? = null

    private var onConfirm: (() -> Unit)? = null
    private var onSecondary: (() -> Unit)? = null
    private var onDismiss: ((Boolean) -> Unit)? = null
    private var forceDarkTheme: Boolean = false
    private var displayConfirmButtonFirst: Boolean = false
    private var removeSecondaryButtonBorder: Boolean = false
    private var summaryTextSize: Float? = null
    private var isDismissedWithoutAction = true

    @Inject lateinit var theme: Theme

    companion object {
        fun downloadWarningDialog(episodeCount: Int, resources: Resources, onConfirm: () -> Unit): ConfirmationDialog? {
            return if (episodeCount < 5) {
                onConfirm()
                null
            } else if (episodeCount in 5..Settings.MAX_DOWNLOAD) {
                ConfirmationDialog()
                    .setButtonType(ButtonType.Normal(resources.getString(LR.string.download_warning_button, episodeCount)))
                    .setIconId(IR.drawable.ic_download)
                    .setTitle(resources.getString(LR.string.download_warning_title))
                    .setOnConfirm(onConfirm)
            } else {
                ConfirmationDialog()
                    .setButtonType(ButtonType.Normal(resources.getString(LR.string.download_warning_button, Settings.MAX_DOWNLOAD)))
                    .setIconId(IR.drawable.ic_download)
                    .setTitle(resources.getString(LR.string.download_warning_title))
                    .setSummary(resources.getString(LR.string.download_warning_limit_summary, Settings.MAX_DOWNLOAD))
                    .setOnConfirm(onConfirm)
            }
        }

        fun deleteDownloadWarningDialog(episodeCount: Int, warningLimit: Int, resources: Resources, onConfirm: () -> Unit): ConfirmationDialog? {
            return if (episodeCount <= warningLimit) {
                onConfirm()
                null
            } else {
                ConfirmationDialog()
                    .setIconId(IR.drawable.ic_delete)
                    .setTitle(resources.getString(LR.string.delete_download_warning_title))
                    .setSummary(resources.getString(LR.string.delete_download_warning_summary))
                    .setButtonType(ButtonType.Danger(resources.getString(LR.string.delete_download_warning_button, episodeCount)))
                    .setOnConfirm(onConfirm)
            }
        }
    }

    fun setForceDarkTheme(force: Boolean): ConfirmationDialog {
        this.forceDarkTheme = force
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (!forceDarkTheme || theme.isDarkTheme) {
            return super.onCreateDialog(savedInstanceState)
        }

        val context = ContextThemeWrapper(requireContext(), UR.style.ThemeDark)
        return BottomSheetDialog(context, UR.style.BottomSheetDialogThemeDark)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val dialogContext = requireDialog().context
        val themeType = if (forceDarkTheme && !theme.isDarkTheme) Theme.ThemeType.DARK else theme.activeTheme
        AppTheme(themeType) {
            ConfirmationDialogContent(
                title = title,
                summary = summary,
                icon = remember { iconDrawable(dialogContext) },
                buttonType = buttonType,
                secondaryType = secondaryType,
                onConfirm = {
                    onConfirm?.invoke()
                    isDismissedWithoutAction = false
                    dismiss()
                },
                onSecondary = {
                    onSecondary?.invoke()
                    isDismissedWithoutAction = false
                    dismiss()
                },
                summaryColor = summaryTextColorAttr?.let { Color(dialogContext.getThemeColor(it)) } ?: MaterialTheme.theme.colors.primaryText02,
                summaryTextSize = summaryTextSize?.sp,
                secondaryTextColor = secondaryTextColorAttr?.let { Color(dialogContext.getThemeColor(it)) },
                displayConfirmButtonFirst = displayConfirmButtonFirst,
                removeSecondaryButtonBorder = removeSecondaryButtonBorder,
            )
        }
    }

    private fun iconDrawable(context: Context): Drawable? {
        if (iconId == 0) return null
        val drawable = AppCompatResources.getDrawable(context, iconId)?.mutate() ?: return null
        iconTintAttr?.let { drawable.setTint(context.getThemeColor(it)) }
        return drawable
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.skipCollapsed = true
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss?.invoke(isDismissedWithoutAction)
    }

    fun setTitle(title: String): ConfirmationDialog {
        this.title = title
        return this
    }

    fun setSummary(summary: String): ConfirmationDialog {
        this.summary = summary
        return this
    }

    fun setIconId(@DrawableRes iconId: Int): ConfirmationDialog {
        this.iconId = iconId
        return this
    }

    fun setButtonType(buttonType: ButtonType): ConfirmationDialog {
        this.buttonType = buttonType
        return this
    }

    fun setSecondaryButtonType(buttonType: ButtonType): ConfirmationDialog {
        this.secondaryType = buttonType
        return this
    }

    fun setOnConfirm(onConfirm: () -> Unit): ConfirmationDialog {
        this.onConfirm = onConfirm
        return this
    }

    fun setOnSecondary(onSecondary: () -> Unit): ConfirmationDialog {
        this.onSecondary = onSecondary
        return this
    }

    fun setIconTint(@AttrRes colorAttr: Int?): ConfirmationDialog {
        this.iconTintAttr = colorAttr
        return this
    }

    fun setOnDismiss(onDismiss: ((isDismissedWithoutAction: Boolean) -> Unit)?): ConfirmationDialog {
        this.onDismiss = onDismiss
        return this
    }

    fun setDisplayConfirmButtonFirst(displayConfirmButtonFirst: Boolean): ConfirmationDialog {
        this.displayConfirmButtonFirst = displayConfirmButtonFirst
        return this
    }

    fun setSecondaryTextColor(@AttrRes colorAttr: Int?): ConfirmationDialog {
        this.secondaryTextColorAttr = colorAttr
        return this
    }

    fun setSummaryTextColor(@AttrRes colorAttr: Int?): ConfirmationDialog {
        this.summaryTextColorAttr = colorAttr
        return this
    }

    fun setRemoveSecondaryButtonBorder(removeSecondaryButtonBorder: Boolean): ConfirmationDialog {
        this.removeSecondaryButtonBorder = removeSecondaryButtonBorder
        return this
    }

    fun setSummaryTextSize(size: Float): ConfirmationDialog {
        this.summaryTextSize = size
        return this
    }

    override fun onPause() {
        super.onPause()
        this.dismissAllowingStateLoss()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag)
        }
    }
}
