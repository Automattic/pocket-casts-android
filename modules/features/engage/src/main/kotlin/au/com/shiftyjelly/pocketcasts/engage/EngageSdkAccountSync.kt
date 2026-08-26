package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.external.ExternalDataManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.android.engage.common.datamodel.ClusterType
import com.google.android.engage.service.AppEngagePublishClient
import com.google.android.engage.service.ServiceAvailabilityRequest
import kotlinx.coroutines.CancellationException
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
    private val client = AppEngagePublishClient(context)
    private val service = ClusterService(context, client)
    private var syncJob: Job? = null

    fun keepAccountInSync() {
        if (syncJob != null) {
            return
        }
        syncJob = coroutineScope.launch {
            syncManager.isLoggedInObservable.asFlow().collectLatest { isSignedIn ->
                try {
                    val request = ServiceAvailabilityRequest.Builder()
                        .addIntendedClusterType(ClusterType.TYPE_ENGAGEMENT)
                        .build()
                    val isAvailableMap = client.isServiceAvailable(request).await()
                    if (isAvailableMap[ClusterType.TYPE_ENGAGEMENT] == true) {
                        val data = dataManager.getEngageData(isSignedIn)
                        service.updateUserAccount(data).await()
                    }
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }
}
