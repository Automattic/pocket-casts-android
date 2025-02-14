package au.com.shiftyjelly.pocketcasts.repositories.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncSettingsTask
import javax.inject.Inject

class CastsWorkerFactory @Inject constructor(
    val syncManager: SyncManager,
    val settings: Settings,
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        val workerKlass = Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
        val constructor = workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is SyncSettingsTask -> {
                instance.namedSettingsCaller = syncManager
                instance.settings = settings
            }
        }

        return instance
    }
}
