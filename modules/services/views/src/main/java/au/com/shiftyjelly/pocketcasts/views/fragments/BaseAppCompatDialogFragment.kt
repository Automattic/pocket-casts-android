package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
open class BaseAppCompatDialogFragment : AppCompatDialogFragment(), CoroutineScope {

    open val statusBarColor: StatusBarColor? = StatusBarColor.Light

    @Inject
    lateinit var theme: Theme

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view.background == null) {
            view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        }
        view.isClickable = true

        val activity = activity
        val statusBarColor = statusBarColor
        if (activity != null && statusBarColor != null) {
            theme.updateWindowStatusBar(
                window = activity.window,
                statusBarColor = statusBarColor,
                context = activity
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? FragmentHostListener)?.updateStatusBar()
    }
}
