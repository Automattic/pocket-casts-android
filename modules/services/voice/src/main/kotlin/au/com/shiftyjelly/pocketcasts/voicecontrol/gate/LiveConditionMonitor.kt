package au.com.shiftyjelly.pocketcasts.voicecontrol.gate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.BatteryOkCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.NotCastingCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.NotOnCallCondition
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Bridges Android system callbacks (phone state, power save mode, cast state)
 * into the mutable transient conflict conditions so they stay current.
 */
@Singleton
class LiveConditionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val castManager: CastManager,
    private val notOnCallCondition: NotOnCallCondition,
    private val batteryOkCondition: BatteryOkCondition,
    private val notCastingCondition: NotCastingCondition,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val callbackExecutor = Executors.newSingleThreadExecutor()
    private var started = false

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pm = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSave = pm?.isPowerSaveMode == true
            Timber.i("Power save mode changed: %s", isPowerSave)
            batteryOkCondition.updatePowerSaveMode(isPowerSave)
        }
    }

    private val callStateCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            val inCall = state == TelephonyManager.CALL_STATE_OFFHOOK
            Timber.i("Call state changed: inCall=%s", inCall)
            notOnCallCondition.updateInCall(inCall)
        }
    }

    fun start() {
        if (started) return
        started = true

        // Phone call state
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.registerTelephonyCallback(callbackExecutor, callStateCallback)
        } catch (e: SecurityException) {
            Timber.w(e, "Cannot listen for phone state — permission not granted")
        }

        // Power save mode
        context.registerReceiver(
            powerSaveReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )

        // Cast state
        castManager.isConnectedFlow
            .onEach { isCasting -> notCastingCondition.updateCasting(isCasting) }
            .launchIn(scope)
    }

    fun stop() {
        if (!started) return
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.unregisterTelephonyCallback(callStateCallback)
        } catch (_: SecurityException) {
            // ignore
        }
        try {
            context.unregisterReceiver(powerSaveReceiver)
        } catch (_: IllegalArgumentException) {
            // not registered
        }
    }
}
