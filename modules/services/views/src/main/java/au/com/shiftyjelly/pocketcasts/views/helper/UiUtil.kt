package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import au.com.shiftyjelly.pocketcasts.ui.extensions.inPortrait
import au.com.shiftyjelly.pocketcasts.views.R
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object UiUtil {

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @Suppress("DEPRECATION")
    fun hideProgressDialog(progressDialog: android.app.ProgressDialog?) {
        try {
            if (progressDialog != null && progressDialog.isShowing) {
                progressDialog.dismiss()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun displayAlert(context: Context?, title: String, message: String, onComplete: Runnable?) {
        displayAlert(context, -1, title, message, onComplete)
    }

    fun displayAlert(context: Context?, icon: Int, title: String, message: String, onComplete: Runnable?) {
        context ?: return
        try {
            var builder = AlertDialog.Builder(context)
            if (icon > 0) {
                builder = builder.setIcon(icon)
            }

            builder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(LR.string.ok, null)
                .setOnDismissListener { onComplete?.run() }
                .show()
        } catch (e: Exception) {
            // you can get exceptions sometimes, like if you try to launch a message from an invalid activity
            Timber.e(e)
        }
    }

    fun displayAlertError(context: Context?, title: String, message: String, onComplete: Runnable?) {
        displayAlert(context, title, message, onComplete)
    }

    fun displayAlertError(context: Context?, message: String, onComplete: Runnable?) {
        displayAlertError(context, context?.getString(LR.string.error) ?: "", message, onComplete)
    }

    fun setTitle(menu: Menu?, menuItemId: Int, title: String) {
        if (menu == null) return

        val item = menu.findItem(menuItemId) ?: return

        item.title = title
    }

    fun setBackgroundColor(view: View?, color: Int) {
        if (view == null) {
            return
        }
        view.setBackgroundColor(color)
    }

    fun colorIntToHexString(colorInt: Int): String {
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }

    fun getGridColumnCount(smallArtwork: Boolean, context: Context?): Int {
        context ?: return 1
        val contentViewWidthDp = getWindowWidthDp(context)
        return getGridColumnCount(contentViewWidthDp, context.resources.configuration.inPortrait(), smallArtwork)
    }

    fun getDiscoverGridColumnCount(context: Context?): Int {
        context ?: return 2
        val contentViewWidthDp = getWindowWidthDp(context)
        val inPortrait = context.resources.configuration.inPortrait()
        return if (inPortrait) {
            when {
                contentViewWidthDp > 700 -> 4
                contentViewWidthDp > 500 -> 3
                else -> 2
            }
        } else {
            when {
                contentViewWidthDp > 900 -> 6
                contentViewWidthDp > 700 -> 5
                else -> 4
            }
        }
    }

    fun getDiscoverGridImageWidthPx(context: Context): Int {
        val columns = getDiscoverGridColumnCount(context)
        val padding = (columns + 1) * (16f * context.resources.displayMetrics.density)
        return ((getWindowWidthPx(context) - padding) / columns).toInt()
    }

    fun getWindowWidthPx(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    fun getWindowWidthDp(context: Context): Int {
        return (getWindowWidthPx(context) / getDensity(context)).toInt()
    }

    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    private fun getGridColumnCount(contentViewWidthDp: Int, portrait: Boolean, smallArtwork: Boolean): Int {
        val num: Int

        if (portrait) {
            if (contentViewWidthDp < 500) {
                num = if (smallArtwork) 4 else 3
            } else if (contentViewWidthDp < 700) {
                num = if (smallArtwork) 5 else 4
            } else {
                num = if (smallArtwork) 6 else 5
            }
        } else {
            num = if (smallArtwork) 6 else 5
        }

        return num
    }

    fun getGridImageWidthPx(smallArtwork: Boolean, context: Context): Int {
        val columnCount = getGridColumnCount(smallArtwork, context)
        val contentWidthPx = getWindowWidthPx(context)
        // add the spacing between the columns and the padding on the sides of the grid
        val resources = context.resources
        val gridItemPadding = resources.getDimensionPixelSize(R.dimen.grid_item_padding)
        val gridOuterPadding = resources.getDimensionPixelSize(R.dimen.grid_outer_padding)
        val spacingWidth = ((columnCount - 1) * gridItemPadding) + (2 * gridOuterPadding)
        return ((contentWidthPx.toFloat() - spacingWidth) / columnCount).toInt()
    }

    fun displayDialogNoEmailApp(context: Context) {
        displayAlertError(context, context.getString(LR.string.settings_no_email_app_title), context.getString(LR.string.settings_no_email_app), null)
    }
}
