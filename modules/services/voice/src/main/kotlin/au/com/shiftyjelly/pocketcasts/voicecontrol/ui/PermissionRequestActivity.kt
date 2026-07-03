package au.com.shiftyjelly.pocketcasts.voicecontrol.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Transparent activity that requests RECORD_AUDIO permission and finishes.
 * Launched by VoiceControlServiceController when permission is missing.
 */
class PermissionRequestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            finish()
            return
        }

        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { finish() }

        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
