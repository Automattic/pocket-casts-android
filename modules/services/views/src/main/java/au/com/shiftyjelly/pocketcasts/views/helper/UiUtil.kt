package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Context
import android.content.res.Configuration
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import au.com.shiftyjelly.pocketcasts.ui.extensions.inPortrait
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object UiUtil {

    private const val MINIMUM_DIMENSION_DP_FOR_NAVIGATION_DRAWER = 600

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
        val contentViewWidthDp = getContentViewWidthDp(context)
        val isDrawerHidden = isNavigationDrawerHidden(context)
        return getGridColumnCount(contentViewWidthDp, context.resources.configuration.inPortrait(), isDrawerHidden, smallArtwork)
    }

    fun getDiscoverGridColumnCount(context: Context?): Int {
        context ?: return 2
        val contentViewWidthDp = getContentViewWidthDp(context)
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
        return ((getContentViewWidthPx(context) - padding) / columns).toInt()
    }

    fun getWindowWidthPx(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    fun getWindowWidthDp(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (displayMetrics.widthPixels * displayMetrics.density).toInt()
    }

    fun getWindowHeightPx(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    fun getWindowHeightDp(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (displayMetrics.heightPixels * displayMetrics.density).toInt()
    }

    fun getContentViewWidthPx(context: Context): Int {
        var windowWidthPx = getWindowWidthPx(context)
        val drawerHidden = isNavigationDrawerHidden(context)
        if (!drawerHidden) {
            windowWidthPx -= getNavigationDrawerWidthPx(context)
        }
        return windowWidthPx
    }

    fun getContentViewWidthDp(context: Context): Int {
        return (getContentViewWidthPx(context) / getDensity(context)).toInt()
    }

    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    private fun getGridColumnCount(contentViewWidthDp: Int, portrait: Boolean, isNavigationDrawerHidden: Boolean, smallArtwork: Boolean): Int {
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
            if (isNavigationDrawerHidden) {
                num = if (smallArtwork) 6 else 5
            } else {
                if (contentViewWidthDp > 700) {
                    num = if (smallArtwork) 7 else 6
                } else {
                    num = if (smallArtwork) 6 else 5
                }
            }
        }

        return num
    }

    fun getGridImageWidthPx(smallArtwork: Boolean, context: Context): Int {
        val columnCount = getGridColumnCount(smallArtwork, context)
        val contentWidthPx = getContentViewWidthPx(context)
        return (contentWidthPx.toFloat() / columnCount).toInt()
    }

    fun isNavigationDrawerHidden(context: Context): Boolean {
        val config = context.resources.configuration
        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        val height = (displayMetrics.heightPixels / displayMetrics.density).toInt()
        return width < MINIMUM_DIMENSION_DP_FOR_NAVIGATION_DRAWER || height < MINIMUM_DIMENSION_DP_FOR_NAVIGATION_DRAWER || config.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun getNavigationDrawerWidthPx(context: Context): Int {
        return getNavigationDrawerWidthPx(getWindowWidthPx(context), getDensity(context))
    }

    private fun getNavigationDrawerWidthPx(windowWidthPx: Int, density: Float): Int {
        val windowWidthDp = (windowWidthPx / density).toInt()

        val defaultDrawerWidthDp = 330
        val defaultDrawerWidthPx = (defaultDrawerWidthDp * density).toInt()

        // thin screen use most of the screen
        if (windowWidthDp < 350) {
            return (windowWidthPx * 0.9f).toInt()
        } else if (windowWidthDp < 370) {
            return (windowWidthPx * 0.85f).toInt()
        }

        return defaultDrawerWidthPx
    }

    fun displayDialogNoEmailApp(context: Context) {
        displayAlertError(context, context.getString(LR.string.settings_no_email_app_title), context.getString(LR.string.settings_no_email_app), null)
    }
}
