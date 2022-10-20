package au.com.shiftyjelly.pocketcasts.views.component

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.views.R

class CustomTabsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.data?.let { uri ->
            val clipData = ClipData.newUri(null, uri.toString(), uri)
            val clipBoardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipBoardManager.setPrimaryClip(clipData)

            Toast.makeText(
                context,
                context.getString(R.string.copied_to_clipboard),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}