package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import timber.log.Timber

fun GoogleApiAvailability.isGooglePlayServicesAvailableSuccess(context: Context, exceptionDefault: Boolean = false): Boolean {
    return try {
        isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    } catch (ex: Exception) {
        Timber.w("Unable to check if Google Play Services is installed.", ex)
        exceptionDefault
    }
}
