package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.nova.ExternalDataManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.android.engage.service.AppEngagePublishClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.tasks.await

class EngageSdkAccountSync(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val dataManager: ExternalDataManager,
    private val syncManager: SyncManager,
) {
    private val service = ClusterService(context, AppEngagePublishClient(context))
    private var syncJob: Job? = null

    fun keepAccountInSync() {
        if (syncJob != null) {
            return
        }
        syncJob = coroutineScope.launch {
            syncManager.isLoggedInObservable.asFlow().collectLatest { isSignedIn ->
                val data = dataManager.getEngageData(isSignedIn)
                service.updateUserAccount(data).await()
            }
        }
    }
}
