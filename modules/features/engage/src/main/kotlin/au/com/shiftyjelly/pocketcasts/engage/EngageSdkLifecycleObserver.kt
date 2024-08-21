package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

internal class EngageSdkLifecycleObserver(
    private val context: Context,
) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        EngageSdkWorkers.enqueueOneOffWork(context)
    }

    companion object {
        fun register(context: Context) {
            val observer = EngageSdkLifecycleObserver(context)
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
    }
}
