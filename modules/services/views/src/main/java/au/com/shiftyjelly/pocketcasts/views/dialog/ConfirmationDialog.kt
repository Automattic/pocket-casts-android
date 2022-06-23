package au.com.shiftyjelly.pocketcasts.views.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.databinding.FragmentConfirmationBinding
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
    private lateinit var buttonType: ConfirmationDialog.ButtonType
    var secondaryType: ConfirmationDialog.ButtonType? = null
    @AttrRes private var iconTintAttr: Int? = UR.attr.primary_icon_01
    private var onConfirm: (() -> Unit)? = null
    private var onSecondary: (() -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null
    private var forceDarkTheme: Boolean = false
    private var binding: FragmentConfirmationBinding? = null

    @Inject lateinit var theme: Theme

    companion object {
        fun downloadWarningDialog(episodeCount: Int, resources: Resources, onConfirm: () -> Unit): ConfirmationDialog? {
            return if (episodeCount < 5) {
                onConfirm()
                null
            } else if (episodeCount in 5..Settings.MAX_DOWNLOAD) {
                ConfirmationDialog()
                    .setButtonType(ConfirmationDialog.ButtonType.Normal(resources.getString(LR.string.download_warning_button, episodeCount)))
                    .setIconId(IR.drawable.ic_download)
                    .setTitle(resources.getString(LR.string.download_warning_title))
                    .setOnConfirm(onConfirm)
            } else {
                ConfirmationDialog()
                    .setButtonType(ConfirmationDialog.ButtonType.Normal(resources.getString(LR.string.download_warning_button, Settings.MAX_DOWNLOAD)))
                    .setIconId(IR.drawable.ic_download)
                    .setTitle(resources.getString(LR.string.download_warning_title))
                    .setSummary(resources.getString(LR.string.download_warning_limit_summary, Settings.MAX_DOWNLOAD))
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentConfirmationBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.skipCollapsed = true
        }

        val context = view.context

        binding.lblTitle.isVisible = title != null
        binding.lblTitle.text = title
        binding.lblSummary.isVisible = summary != null
        binding.lblSummary.text = summary
        binding.imgIcon.setImageResource(iconId)
        iconTintAttr?.let { binding.imgIcon.imageTintList = ColorStateList.valueOf(context.getThemeColor(it)) }
        val btnConfirm = binding.btnConfirm
        btnConfirm.text = buttonType.text
        btnConfirm.setOnClickListener {
            onConfirm?.invoke()
            dismiss()
        }

        val dangerColor = context.getThemeColor(UR.attr.support_05)
        val defaultColor = context.getThemeColor(UR.attr.support_01)

        val buttonColor = if (buttonType is ButtonType.Danger) dangerColor else defaultColor
        btnConfirm.backgroundTintList = ColorStateList.valueOf(buttonColor)

        val secondaryButtonColor = if (secondaryType is ButtonType.Danger) dangerColor else defaultColor
        val btnSecondary = binding.btnSecondary
        btnSecondary.strokeColor = ColorStateList.valueOf(secondaryButtonColor)
        btnSecondary.strokeWidth = 2.dpToPx(context)
        btnSecondary.setTextColor(btnSecondary.strokeColor)
        btnSecondary.isVisible = secondaryType != null
        btnSecondary.text = secondaryType?.text
        btnSecondary.setOnClickListener {
            onSecondary?.invoke()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss?.invoke()
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

    fun setOnDismiss(onDismiss: (() -> Unit)?): ConfirmationDialog {
        this.onDismiss = onDismiss
        return this
    }

    override fun onPause() {
        super.onPause()
        this.dismissAllowingStateLoss()
    }
}
