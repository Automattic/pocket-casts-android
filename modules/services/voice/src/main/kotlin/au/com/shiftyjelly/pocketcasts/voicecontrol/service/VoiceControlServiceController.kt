package au.com.shiftyjelly.pocketcasts.voicecontrol.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import au.com.shiftyjelly.pocketcasts.repositories.playback.AppLifecycleProvider
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@Singleton
class VoiceControlServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLifecycleProvider: AppLifecycleProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isMonitoring = false
    private var serviceStarted = false

    fun start() {
        if (serviceStarted) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("VoiceControlServiceController: RECORD_AUDIO not granted, requesting")
            context.startActivity(
                Intent(context, au.com.shiftyjelly.pocketcasts.voicecontrol.ui.PermissionRequestActivity::class.java)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            )
            return
        }
        Timber.i("VoiceControlServiceController: starting service")
        serviceStarted = true
        context.startForegroundService(Intent(context, VoiceControlService::class.java))
    }

    fun stop() {
        serviceStarted = false
        Timber.i("VoiceControlServiceController: stopping service")
        context.stopService(Intent(context, VoiceControlService::class.java))
    }

    fun startMonitoring(gate: VoiceControlGate) {
        if (isMonitoring) return
        isMonitoring = true
        Timber.i("VoiceControlServiceController: starting gate monitoring")

        combine(gate.state, appLifecycleProvider.isInForeground) { gateState, foreground ->
            gateState to foreground
        }.onEach { (gateState, foreground) ->
            if (gateState.allowed && !serviceStarted) {
                Timber.i("VoiceControlServiceController: gate allowed, starting service")
                start()
            }
        }.launchIn(scope)
    }

    fun stopMonitoring() {
        isMonitoring = false
        scope.cancel()
    }
}
