package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Context
import android.content.Intent

sealed interface DeepLink {
    fun toIntent(context: Context): Intent
}
