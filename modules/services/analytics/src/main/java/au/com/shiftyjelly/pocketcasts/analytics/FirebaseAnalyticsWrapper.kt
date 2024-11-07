package au.com.shiftyjelly.pocketcasts.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * A wrapper around Firebase Analytics to allow for mocking as the class is final.
 */
open class FirebaseAnalyticsWrapper(private val firebaseAnalytics: FirebaseAnalytics) {
    open fun logEvent(name: String, params: Bundle?) {
        firebaseAnalytics.logEvent(name, params)
    }
}
