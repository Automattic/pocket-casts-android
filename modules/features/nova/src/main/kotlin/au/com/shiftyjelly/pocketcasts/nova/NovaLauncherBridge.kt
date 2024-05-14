package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovaLauncherBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val isNovaLauncherIntegrationEnabled = AtomicBoolean(false)
    private val areSyncRulesActive = AtomicBoolean(false)
    private val processLifecycle = ProcessLifecycleOwner.get().lifecycle
    private val novaLauncherObserver = NovaLauncherSyncLifecycleObserver(context)
    private val registrationObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = refreshSyncRules()
        override fun onStop(owner: LifecycleOwner) = refreshSyncRules()
    }

    fun monitorNovaLauncherIntegration() {
        if (!isNovaLauncherIntegrationEnabled.getAndSet(true)) {
            processLifecycle.addObserver(registrationObserver)
        }
    }

    private fun refreshSyncRules() {
        if (!FeatureFlag.isEnabled(Feature.NOVA_LAUNCHER)) {
            unregisterNovaLauncherSync()
            return
        }
        if (!context.isNovaLauncherInstalled) {
            unregisterNovaLauncherSync()
            areSyncRulesActive.set(false)
            return
        }
        if (context.isNovaLauncherInstalled && !areSyncRulesActive.getAndSet(true)) {
            registerNovaLauncherSync()
        }
    }

    private fun registerNovaLauncherSync() {
        processLifecycle.addObserver(novaLauncherObserver)
        NovaLauncherSyncWorker.enqueuePeriodicWork(context)
    }

    private fun unregisterNovaLauncherSync() {
        processLifecycle.removeObserver(novaLauncherObserver)
        NovaLauncherSyncWorker.cancelOneOffWork(context)
        NovaLauncherSyncWorker.cancelPeriodicWork(context)
    }
}
