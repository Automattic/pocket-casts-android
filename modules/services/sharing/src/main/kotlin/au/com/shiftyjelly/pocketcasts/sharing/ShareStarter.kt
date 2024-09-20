package au.com.shiftyjelly.pocketcasts.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent

interface ShareStarter {
    fun start(context: Context, intent: Intent)

    fun copyLink(context: Context, data: ClipData)
}
