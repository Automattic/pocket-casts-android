package au.com.shiftyjelly.pocketcasts.views.component;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class CustomTabsBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newUri(null, uri.toString(), uri);
            clipboardManager.setPrimaryClip(clipData);

            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}