package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class NovaLauncherSyncLifecycleObserver(
    private val context: Context,
) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = NovaLauncherSyncWorker.enqueueOneOffWork(context)
}
