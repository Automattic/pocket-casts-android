package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import android.content.Intent

fun interface ShareStarter {
    fun start(context: Context, intent: Intent)
}
